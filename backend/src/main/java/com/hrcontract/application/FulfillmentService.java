package com.hrcontract.application;

import com.hrcontract.application.ContractModels.CreateFulfillmentMilestoneRequest;
import com.hrcontract.application.ContractModels.CreatePaymentPlanRequest;
import com.hrcontract.application.ContractModels.FulfillmentMilestone;
import com.hrcontract.application.ContractModels.PaymentPlan;
import com.hrcontract.application.ContractModels.PaymentRecord;
import com.hrcontract.application.ContractModels.CreatePaymentRecordRequest;
import com.hrcontract.application.ContractModels.UpdateFulfillmentStatusRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class FulfillmentService {
    private final JdbcTemplate jdbc;
    private final ContractService contracts;

    public FulfillmentService(JdbcTemplate jdbc, ContractService contracts) { this.jdbc = jdbc; this.contracts = contracts; }

    @PostConstruct
    @Profile("!mysql")
    void initializeSchema() {
        if (mysqlProfile()) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_payment_plan (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, item_name TEXT NOT NULL, direction TEXT NOT NULL, amount NUMERIC NOT NULL, due_date TEXT NOT NULL, status TEXT NOT NULL, paid_at TEXT, remark TEXT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_fulfillment_milestone (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, name TEXT NOT NULL, due_date TEXT NOT NULL, status TEXT NOT NULL, completed_at TEXT, remark TEXT)");
        ensureColumn("contract_payment_plan", "phase_no", "INTEGER NOT NULL DEFAULT 1");
        ensureColumn("contract_payment_plan", "trigger_condition", "TEXT");
        ensureColumn("contract_payment_plan", "payment_condition_type", "INTEGER");
        ensureColumn("contract_payment_plan", "paid_amount", "NUMERIC NOT NULL DEFAULT 0");
        ensureColumn("contract_payment_plan", "paid_date", "TEXT");
        ensureColumn("contract_payment_plan", "is_overdue", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("contract_payment_plan", "source_extract_task_id", "INTEGER");
        ensureColumn("contract_payment_plan", "is_ai_confirmed", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("contract_fulfillment_milestone", "item_description", "TEXT");
        ensureColumn("contract_fulfillment_milestone", "item_type", "INTEGER NOT NULL DEFAULT 4");
        ensureColumn("contract_fulfillment_milestone", "responsible_user_id", "INTEGER");
        ensureColumn("contract_fulfillment_milestone", "evidence_attachment_id", "INTEGER");
        ensureColumn("contract_fulfillment_milestone", "actual_finish_date", "TEXT");
        ensureColumn("contract_fulfillment_milestone", "perform_status", "INTEGER NOT NULL DEFAULT 0");
        ensureColumn("contract_fulfillment_milestone", "source_extract_task_id", "INTEGER");
        ensureColumn("contract_fulfillment_milestone", "is_ai_confirmed", "INTEGER NOT NULL DEFAULT 0");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_payment_record (id INTEGER PRIMARY KEY, payment_plan_id INTEGER NOT NULL, amount NUMERIC NOT NULL, payment_date TEXT NOT NULL, status TEXT NOT NULL, voucher_attachment_id INTEGER, remark TEXT, created_at TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_fulfillment_event (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, milestone_id INTEGER NOT NULL, event_type TEXT NOT NULL, remark TEXT, created_at TEXT NOT NULL)");
    }

    public List<PaymentPlan> payments(Long contractId) {
        require(contractId);
        return jdbc.query("SELECT id,contract_id,item_name,direction,amount,due_date,status,paid_at,remark FROM contract_payment_plan WHERE contract_id=? ORDER BY due_date,id", (rs, row) -> new PaymentPlan(rs.getLong("id"), rs.getLong("contract_id"), rs.getString("item_name"), rs.getString("direction"), rs.getBigDecimal("amount"), rs.getString("due_date"), paymentStatus(rs.getString("status"), rs.getString("due_date")), rs.getString("paid_at"), rs.getString("remark")), contractId);
    }

    public PaymentPlan addPayment(Long contractId, CreatePaymentPlanRequest request) {
        requireActive(contractId);
        if (request == null || blank(request.itemName()) || blank(request.dueDate()) || request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) throw bad("付款计划信息不完整");
        String direction = request.direction() == null ? "PAY" : request.direction();
        if (!List.of("PAY", "RECEIVE").contains(direction)) throw bad("收付方向无效");
        parseDate(request.dueDate());
        long id = nextId("contract_payment_plan", 9000);
        jdbc.update("INSERT INTO contract_payment_plan(id,contract_id,item_name,direction,amount,due_date,status,remark) VALUES(?,?,?,?,?,?,?,?)", id, contractId, request.itemName().trim(), direction, request.amount(), request.dueDate(), "PENDING", request.remark());
        log(contractId, "PAYMENT_PLAN", "新增收付款计划 " + request.itemName().trim());
        return payments(contractId).stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    public PaymentPlan markPayment(Long contractId, Long paymentId, String status) {
        requireActive(contractId);
        if (!List.of("PENDING", "PAID", "CANCELLED").contains(status)) throw bad("付款计划状态无效");
        int changed = jdbc.update("UPDATE contract_payment_plan SET status=?,paid_at=? WHERE id=? AND contract_id=?", status, "PAID".equals(status) ? Instant.now().toString() : null, paymentId, contractId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "付款计划不存在");
        log(contractId, "PAYMENT_STATUS", "收付款计划状态变更为 " + status);
        return payments(contractId).stream().filter(item -> item.id().equals(paymentId)).findFirst().orElseThrow();
    }

    public List<PaymentRecord> paymentRecords(Long contractId, Long paymentId) {
        require(contractId);
        payment(contractId, paymentId);
        return jdbc.query("SELECT id,payment_plan_id,amount,payment_date,status,voucher_attachment_id,remark,created_at FROM contract_payment_record WHERE payment_plan_id=? ORDER BY payment_date,id",
                (rs, row) -> new PaymentRecord(rs.getLong("id"), rs.getLong("payment_plan_id"), rs.getBigDecimal("amount"), rs.getString("payment_date"), rs.getString("status"), (Long) rs.getObject("voucher_attachment_id"), rs.getString("remark"), Instant.parse(rs.getString("created_at"))), paymentId);
    }

    public PaymentRecord addPaymentRecord(Long contractId, Long paymentId, CreatePaymentRecordRequest request) {
        requireActive(contractId);
        PaymentPlan plan = payment(contractId, paymentId);
        if (request == null || request.amount() == null || request.amount().signum() <= 0 || blank(request.paymentDate())) throw bad("实际收付款金额和日期不能为空");
        parseDate(request.paymentDate());
        if (request.voucherAttachmentId() != null) {
            Integer attachment = jdbc.queryForObject("SELECT COUNT(*) FROM contract_attachment a JOIN contract_payment_plan p ON p.contract_id=a.contract_id WHERE a.id=? AND p.id=?", Integer.class, request.voucherAttachmentId(), paymentId);
            if (attachment == null || attachment == 0) throw bad("付款凭证必须属于当前合同");
        }
        BigDecimal paid = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM contract_payment_record WHERE payment_plan_id=? AND status='CONFIRMED'", BigDecimal.class, paymentId);
        if (paid.add(request.amount()).compareTo(plan.amount()) > 0) throw bad("实际收付款累计金额不能超过计划金额");
        long id = nextId("contract_payment_record", 10000);
        Instant now = Instant.now();
        jdbc.update("INSERT INTO contract_payment_record(id,payment_plan_id,amount,payment_date,status,voucher_attachment_id,remark,created_at) VALUES(?,?,?,?,?,?,?,?)", id, paymentId, request.amount(), request.paymentDate(), "CONFIRMED", request.voucherAttachmentId(), request.remark(), now.toString());
        BigDecimal total = paid.add(request.amount());
        String planStatus = total.compareTo(plan.amount()) == 0 ? "PAID" : "PARTIAL";
        jdbc.update("UPDATE contract_payment_plan SET paid_amount=?,paid_date=?,paid_at=?,status=? WHERE id=?", total, request.paymentDate(), now.toString(), planStatus, paymentId);
        log(contractId, "PAYMENT_RECORD", "登记实际" + ("PAY".equals(plan.direction()) ? "付款" : "收款") + " " + request.amount());
        return paymentRecords(contractId, paymentId).stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    public List<FulfillmentMilestone> milestones(Long contractId) {
        require(contractId);
        return jdbc.query("SELECT id,contract_id,name,due_date,status,completed_at,remark FROM contract_fulfillment_milestone WHERE contract_id=? ORDER BY due_date,id", (rs, row) -> new FulfillmentMilestone(rs.getLong("id"), rs.getLong("contract_id"), rs.getString("name"), rs.getString("due_date"), milestoneStatus(rs.getString("status"), rs.getString("due_date")), rs.getString("completed_at"), rs.getString("remark")), contractId);
    }

    public FulfillmentMilestone addMilestone(Long contractId, CreateFulfillmentMilestoneRequest request) {
        requireActive(contractId);
        if (request == null || blank(request.name()) || blank(request.dueDate())) throw bad("履行节点信息不完整");
        parseDate(request.dueDate());
        long id = nextId("contract_fulfillment_milestone", 9500);
        jdbc.update("INSERT INTO contract_fulfillment_milestone(id,contract_id,name,due_date,status,remark) VALUES(?,?,?,?,?,?)", id, contractId, request.name().trim(), request.dueDate(), "PENDING", request.remark());
        log(contractId, "FULFILLMENT_MILESTONE", "新增履行节点 " + request.name().trim());
        return milestones(contractId).stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    public FulfillmentMilestone completeMilestone(Long contractId, Long milestoneId) {
        return updateMilestoneStatus(contractId, milestoneId, new UpdateFulfillmentStatusRequest("COMPLETED", null));
    }

    public FulfillmentMilestone updateMilestoneStatus(Long contractId, Long milestoneId, UpdateFulfillmentStatusRequest request) {
        requireActive(contractId);
        if (request == null || !List.of("PENDING", "IN_PROGRESS", "COMPLETED", "DELAYED", "EXCEPTION", "CANCELLED").contains(request.status())) throw bad("履约节点状态无效");
        if (("DELAYED".equals(request.status()) || "EXCEPTION".equals(request.status())) && blank(request.remark())) throw bad("延期或异常必须说明原因");
        String completedAt = "COMPLETED".equals(request.status()) ? Instant.now().toString() : null;
        int changed = jdbc.update("UPDATE contract_fulfillment_milestone SET status=?,completed_at=?,actual_finish_date=?,remark=COALESCE(?,remark) WHERE id=? AND contract_id=?", request.status(), completedAt, completedAt == null ? null : LocalDate.now().toString(), request.remark(), milestoneId, contractId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "履行节点不存在");
        long eventId = nextId("contract_fulfillment_event", 11000);
        jdbc.update("INSERT INTO contract_fulfillment_event(id,contract_id,milestone_id,event_type,remark,created_at) VALUES(?,?,?,?,?,?)", eventId, contractId, milestoneId, request.status(), request.remark(), Instant.now().toString());
        log(contractId, "FULFILLMENT_STATUS", "履行节点状态变更为 " + request.status());
        return milestones(contractId).stream().filter(item -> item.id().equals(milestoneId)).findFirst().orElseThrow();
    }

    private PaymentPlan payment(Long contractId, Long paymentId) {
        return payments(contractId).stream().filter(item -> item.id().equals(paymentId)).findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "付款计划不存在"));
    }

    private void require(Long id) { contracts.get(id); }
    private void requireActive(Long id) { if (!"履行中".equals(contracts.get(id).status())) throw bad("合同进入履行中后才能维护履行计划"); }
    private String paymentStatus(String status, String dueDate) { return "PENDING".equals(status) && overdue(dueDate) ? "OVERDUE" : status; }
    private String milestoneStatus(String status, String dueDate) { return "PENDING".equals(status) && overdue(dueDate) ? "OVERDUE" : status; }
    private boolean overdue(String date) { try { return LocalDate.parse(date).isBefore(LocalDate.now()); } catch (RuntimeException e) { return false; } }
    private long nextId(String table, long fallback) { return jdbc.queryForObject("SELECT COALESCE(MAX(id),?)+1 FROM " + table, Long.class, fallback); }
    private void ensureColumn(String table, String column, String definition) {
        try { jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition); }
        catch (Exception ignored) { }
    }
    private void log(Long contractId, String action, String description) {
        jdbc.update("INSERT INTO contract_operation_log(contract_id,action,description,operated_at) VALUES(?,?,?,?)", contractId, action, description, Instant.now().toString());
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private void parseDate(String value) {
        try { LocalDate.parse(value); } catch (RuntimeException exception) { throw bad("日期格式不正确"); }
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private boolean mysqlProfile() { String profiles = System.getenv("SPRING_PROFILES_ACTIVE"); return profiles != null && profiles.contains("mysql"); }
}
