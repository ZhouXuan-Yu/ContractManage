package com.hrcontract.application;

import com.hrcontract.application.ContractModels.AttentionItem;
import com.hrcontract.application.ContractModels.PermissionRole;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Read-only work items derived from contract lifecycle data. */
@Service
public class AttentionService {
    private final JdbcTemplate jdbc;
    private final PermissionAuditService permissions;

    public AttentionService(JdbcTemplate jdbc, PermissionAuditService permissions) {
        this.jdbc = jdbc;
        this.permissions = permissions;
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_attention_disposition (attention_key VARCHAR(255) PRIMARY KEY, contract_id BIGINT NOT NULL, handling_status VARCHAR(32) NOT NULL, handling_opinion TEXT, handled_by VARCHAR(128), handled_at TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_attention_history (id BIGINT PRIMARY KEY, attention_key VARCHAR(255) NOT NULL, contract_id BIGINT NOT NULL, handling_status VARCHAR(32) NOT NULL, handling_opinion TEXT, handled_by VARCHAR(128) NOT NULL, handled_at TEXT NOT NULL)");
    }

    public List<AttentionItem> list(String role, String userId, String orgId, String departmentId) {
        RoleScope scope = scope(role, userId, orgId, departmentId);
        List<AttentionItem> items = new ArrayList<>();
        items.addAll(expiring(scope));
        items.addAll(overduePayments(scope));
        items.addAll(fulfillmentExceptions(scope));
        items.addAll(highRisks(scope));
        return decorate(items).stream().sorted(Comparator.comparingInt((AttentionItem item) -> statusOrder(item.handlingStatus()))
                .thenComparingInt(item -> severityOrder(item.severity()))
                .thenComparing(item -> item.dueDate() == null ? "9999-12-31" : item.dueDate())
                .thenComparing(AttentionItem::contractNo)).limit(100).toList();
    }

    public AttentionItem update(String key, String status, String opinion, String operator, String role, String userId, String orgId, String departmentId) {
        if (!List.of("PROCESSED", "IGNORED", "OPEN").contains(status)) throw bad("待办处理状态不合法");
        AttentionItem item = list(role, userId, orgId, departmentId).stream().filter(candidate -> candidate.key().equals(key)).findFirst().orElseThrow(() -> bad("待办不存在或无权处理"));
        if (!"OPEN".equals(status) && (opinion == null || opinion.isBlank())) throw bad("请填写处理意见");
        Instant now = Instant.now();
        String finalOpinion = opinion == null ? "" : opinion.trim();
        String upsert = mysqlProfile()
                ? "INSERT INTO contract_attention_disposition(attention_key,contract_id,handling_status,handling_opinion,handled_by,handled_at) VALUES(?,?,?,?,?,?) ON DUPLICATE KEY UPDATE handling_status=VALUES(handling_status),handling_opinion=VALUES(handling_opinion),handled_by=VALUES(handled_by),handled_at=VALUES(handled_at)"
                : "INSERT INTO contract_attention_disposition(attention_key,contract_id,handling_status,handling_opinion,handled_by,handled_at) VALUES(?,?,?,?,?,?) ON CONFLICT(attention_key) DO UPDATE SET handling_status=excluded.handling_status,handling_opinion=excluded.handling_opinion,handled_by=excluded.handled_by,handled_at=excluded.handled_at";
        jdbc.update(upsert, key, item.contractId(), status, finalOpinion, operator, now.toString());
        long historyId = jdbc.queryForObject("SELECT COALESCE(MAX(id),0)+1 FROM contract_attention_history", Long.class);
        jdbc.update("INSERT INTO contract_attention_history(id,attention_key,contract_id,handling_status,handling_opinion,handled_by,handled_at) VALUES(?,?,?,?,?,?,?)", historyId, key, item.contractId(), status, finalOpinion, operator, now.toString());
        jdbc.update("INSERT INTO contract_operation_log(contract_id,action,description,operated_at) VALUES(?,?,?,?)", item.contractId(), "ATTENTION_" + status, "待办“" + item.title() + "”" + actionLabel(status) + (finalOpinion.isBlank() ? "" : "：" + finalOpinion), now.toString());
        permissions.audit("待办与告警", "待办" + actionLabel(status), "SUCCESS", operator, item.contractNo() + " - " + item.title());
        return list(role, userId, orgId, departmentId).stream().filter(candidate -> candidate.key().equals(key)).findFirst().orElseThrow();
    }

    private List<AttentionItem> decorate(List<AttentionItem> items) {
        return items.stream().map(item -> {
            String key = item.category() + ":" + item.contractId() + ":" + (item.dueDate() == null ? "-" : item.dueDate());
            List<Disposition> dispositions = jdbc.query("SELECT handling_status,handling_opinion,handled_by,handled_at FROM contract_attention_disposition WHERE attention_key=?", (rs, row) -> new Disposition(rs.getString(1), rs.getString(2), rs.getString(3), Instant.parse(rs.getString(4))), key);
            Disposition disposition = dispositions.isEmpty() ? new Disposition("OPEN", null, null, null) : dispositions.get(0);
            boolean escalated = "HIGH".equals(item.severity()) && item.dueDate() != null && LocalDate.parse(item.dueDate()).isBefore(LocalDate.now().minusDays(2)) && "OPEN".equals(disposition.status());
            return new AttentionItem(key, item.category(), item.severity(), item.contractId(), item.contractNo(), item.contractName(), item.title(), item.detail(), item.dueDate(), item.target(), disposition.status(), disposition.opinion(), disposition.operator(), disposition.operatedAt(), escalated);
        }).toList();
    }

    private List<AttentionItem> expiring(RoleScope scope) {
        String sql = "SELECT c.id,c.contract_no,c.contract_name,c.expire_date FROM t_contract_main c LEFT JOIN contract_draft d ON d.id=c.id "
                + "WHERE c.deleted=0 AND COALESCE(d.archived,0)=0 AND c.contract_status=3 AND c.expire_date IS NOT NULL "
                + "AND c.expire_date >= CURRENT_DATE AND c.expire_date <= date(CURRENT_DATE, '+30 day')" + scope.filter("c");
        return jdbc.query(sql, (rs, row) -> raw("EXPIRING", "MEDIUM", rs.getLong(1), rs.getString(2), rs.getString(3), "合同即将到期", "到期日 " + rs.getString(4), rs.getString(4), "CONTRACT"), scope.args());
    }

    private List<AttentionItem> overduePayments(RoleScope scope) {
        String sql = "SELECT c.id,c.contract_no,c.contract_name,p.plan_name,p.plan_due_date FROM t_contract_payment_plan p "
                + "JOIN t_contract_main c ON c.id=p.contract_id AND c.deleted=0 LEFT JOIN contract_draft d ON d.id=c.id "
                + "WHERE COALESCE(d.archived,0)=0 AND c.contract_status=3 AND p.deleted=0 AND p.pay_status=0 "
                + "AND p.plan_due_date < CURRENT_DATE" + scope.filter("c");
        return jdbc.query(sql, (rs, row) -> raw("PAYMENT_OVERDUE", "HIGH", rs.getLong(1), rs.getString(2), rs.getString(3), "收付款计划逾期", rs.getString(4) + " 已逾期", rs.getString(5), "FULFILLMENT"), scope.args());
    }

    private List<AttentionItem> fulfillmentExceptions(RoleScope scope) {
        String sql = "SELECT c.id,c.contract_no,c.contract_name,m.item_name,m.plan_finish_date,m.perform_status FROM t_contract_perform_item m "
                + "JOIN t_contract_main c ON c.id=m.contract_id AND c.deleted=0 LEFT JOIN contract_draft d ON d.id=c.id "
                + "WHERE COALESCE(d.archived,0)=0 AND c.contract_status=3 AND m.deleted=0 "
                + "AND (m.perform_status NOT IN (0,2,3) OR (m.perform_status=0 AND m.plan_finish_date < CURRENT_DATE))" + scope.filter("c");
        return jdbc.query(sql, (rs, row) -> raw("FULFILLMENT_EXCEPTION", "HIGH", rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(6) == 0 ? "履约节点逾期" : "履约节点异常", rs.getString(4), rs.getString(5), "FULFILLMENT"), scope.args());
    }

    private List<AttentionItem> highRisks(RoleScope scope) {
        String sql = "SELECT c.id,c.contract_no,c.contract_name,MAX(r.review_summary) FROM t_contract_ai_review r "
                + "JOIN t_contract_main c ON c.id=r.contract_id AND c.deleted=0 LEFT JOIN contract_draft d ON d.id=c.id "
                + "WHERE COALESCE(d.archived,0)=0 AND r.deleted=0 AND r.overall_risk_level=3" + scope.filter("c")
                + " GROUP BY c.id,c.contract_no,c.contract_name";
        return jdbc.query(sql, (rs, row) -> raw("AI_HIGH_RISK", "HIGH", rs.getLong(1), rs.getString(2), rs.getString(3), "AI 审查高风险", rs.getString(4) == null ? "请查看风险审查结论" : rs.getString(4), null, "CONTRACT"), scope.args());
    }

    private RoleScope scope(String role, String userId, String orgId, String departmentId) {
        PermissionRole permission = permissions.role(role);
        String filter = "";
        List<Object> args = new ArrayList<>();
        if ("SELF_AND_PARTICIPANTS".equals(permission.dataScope())) { filter += " AND %s.applicant_id=?"; args.add(userId); }
        else if ("ORGANIZATION".equals(permission.dataScope())) { filter += " AND %s.applicant_org_id=?"; args.add(orgId); }
        else if ("DEPARTMENT".equals(permission.dataScope())) { filter += " AND %s.applicant_department_id=?"; args.add(departmentId); }
        else if ("NAMED_CONTRACTS".equals(permission.dataScope())) filter += " AND 1=0";
        if (!permission.typeIds().isEmpty() && !"ADMIN".equals(role)) {
            filter += " AND %s.category_type_id IN (" + String.join(",", java.util.Collections.nCopies(permission.typeIds().size(), "?")) + ")";
            args.addAll(permission.typeIds());
        }
        return new RoleScope(filter, args.toArray());
    }

    private int severityOrder(String severity) { return "HIGH".equals(severity) ? 0 : "MEDIUM".equals(severity) ? 1 : 2; }
    private int statusOrder(String status) { return "OPEN".equals(status) ? 0 : "PROCESSED".equals(status) ? 1 : 2; }
    private AttentionItem raw(String category, String severity, Long contractId, String contractNo, String contractName, String title, String detail, String dueDate, String target) { return new AttentionItem(null, category, severity, contractId, contractNo, contractName, title, detail, dueDate, target, null, null, null, null, false); }
    private String actionLabel(String status) { return switch (status) { case "PROCESSED" -> "已处理"; case "IGNORED" -> "已忽略"; default -> "已重新打开"; }; }
    private org.springframework.web.server.ResponseStatusException bad(String message) { return new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, message); }
    private boolean mysqlProfile() { String profiles = System.getenv("SPRING_PROFILES_ACTIVE"); return profiles != null && profiles.contains("mysql"); }

    private record RoleScope(String template, Object[] args) {
        String filter(String alias) { return template.formatted(alias, alias); }
    }
    private record Disposition(String status, String opinion, String operator, Instant operatedAt) {}
}
