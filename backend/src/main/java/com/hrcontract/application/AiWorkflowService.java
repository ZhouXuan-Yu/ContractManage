package com.hrcontract.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrcontract.application.ContractModels.AiReview;
import com.hrcontract.application.ContractModels.AiTask;
import com.hrcontract.application.ContractModels.ContentVersion;
import com.hrcontract.application.ContractModels.AiDraftRequest;
import com.hrcontract.application.ContractModels.RiskItem;
import com.hrcontract.integration.AiProvider;
import com.hrcontract.integration.ApprovalProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiWorkflowService {
    private final JdbcTemplate jdbc;
    private final ContractService contracts;
    private final AiProvider aiProvider;
    private final ApprovalProvider approvalProvider;
    private final ObjectMapper mapper;

    public AiWorkflowService(JdbcTemplate jdbc, ContractService contracts, AiProvider aiProvider,
                             ApprovalProvider approvalProvider, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.contracts = contracts;
        this.aiProvider = aiProvider;
        this.approvalProvider = approvalProvider;
        this.mapper = mapper;
    }

    @PostConstruct
    void init() {
        if (mysqlProfile()) return;
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_call_log (id INTEGER PRIMARY KEY, contract_id INTEGER, capability TEXT NOT NULL, provider TEXT NOT NULL, model_or_workflow TEXT, status TEXT NOT NULL, error_summary TEXT, created_at TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_task (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, task_id TEXT NOT NULL UNIQUE, task_type TEXT NOT NULL, status TEXT NOT NULL, content_version_id INTEGER NOT NULL, summary TEXT, provider TEXT, raw_response TEXT, created_at TEXT NOT NULL, finished_at TEXT)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS ai_review (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL, task_id INTEGER NOT NULL, review_view TEXT NOT NULL, overall_level TEXT NOT NULL, summary TEXT NOT NULL, risks_json TEXT NOT NULL, provider TEXT, raw_response TEXT, confirmed INTEGER NOT NULL DEFAULT 0, created_at TEXT NOT NULL)");
        jdbc.execute("CREATE TABLE IF NOT EXISTS approval_mock (id INTEGER PRIMARY KEY, contract_id INTEGER NOT NULL UNIQUE, approval_no TEXT NOT NULL UNIQUE, status TEXT NOT NULL, result TEXT, content_version_id INTEGER, content_hash TEXT, approval_generation INTEGER NOT NULL DEFAULT 1, updated_at TEXT NOT NULL)");
        addColumn("ai_task", "provider", "TEXT");
        addColumn("ai_task", "raw_response", "TEXT");
        addColumn("ai_review", "provider", "TEXT");
        addColumn("ai_review", "raw_response", "TEXT");
        addColumn("approval_mock", "content_version_id", "INTEGER");
        addColumn("approval_mock", "content_hash", "TEXT");
        addColumn("approval_mock", "approval_generation", "INTEGER NOT NULL DEFAULT 1");
    }

    public AiTask extract(Long contractId) {
        Context context = context(contractId);
        JsonNode response = call("extract", extractInputs(context));
        String summary = aiProvider.name().equals("mock") ? "Mock extraction completed" : extractionSummary(response);
        return saveTask(contractId, context.version(), "EXTRACT", "COMPLETED", summary, response);
    }

    public JsonNode draft(AiDraftRequest request) {
        if (request == null || blank(request.contractName()) || blank(request.contractType()) || blank(request.partyName()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI 起草至少需要合同名称、合同类型和对方主体");
        Map<String, Object> inputs = new java.util.LinkedHashMap<>();
        inputs.put("contract_name", request.contractName()); inputs.put("contract_type", request.contractType());
        inputs.put("counterparty_name", request.partyName()); inputs.put("our_party_name", request.ourPartyName());
        inputs.put("amount", request.amount()); inputs.put("currency", request.currency());
        inputs.put("start_date", request.startDate()); inputs.put("end_date", request.endDate()); inputs.put("requirements", request.requirements());
        JsonNode response = call("draft", inputs);
        JsonNode result = response.path("answer");
        if (result.isTextual()) {
            try { return mapper.readTree(stripMarkdownFence(result.asText())); }
            catch (JsonProcessingException ignored) { return mapper.createObjectNode().put("content", result.asText()).putArray("warnings").add("AI 未返回结构化变量，请人工核对正文"); }
        }
        return workflowResult(response);
    }

    public AiTask extractAttachment(Long contractId, Long attachmentId) {
        contracts.extractAttachmentText(contractId, attachmentId);
        return extract(contractId);
    }

    public AiReview review(Long contractId, String reviewView) {
        Context context = context(contractId);
        String view = reviewView == null || reviewView.isBlank() ? "OUR_SIDE" : reviewView.trim();
        JsonNode response = call("review", reviewInputs(context, view));
        AiTask task = saveTask(contractId, context.version(), "LEGAL_REVIEW", "COMPLETED", "Legal review completed", response);
        ReviewResult result = aiProvider.name().equals("mock") ? mockReview(context.version()) : parseReview(response);
        long id = nextId("ai_review", 5000);
        Instant now = Instant.now();
        jdbc.update("INSERT INTO ai_review(id,contract_id,task_id,review_view,overall_level,summary,risks_json,provider,raw_response,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",
                id, contractId, task.id(), view, result.level(), result.summary(), write(result.risks()), aiProvider.name(), write(response), now.toString());
        return getReviewByTask(task.id());
    }

    public JsonNode latestExtraction(Long contractId) {
        contracts.get(contractId);
        String raw = jdbc.query("SELECT raw_response FROM ai_task WHERE contract_id=? AND task_type='EXTRACT' ORDER BY id DESC LIMIT 1", (rs, row) -> rs.getString(1), contractId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No extraction result exists"));
        try {
            JsonNode response = mapper.readTree(raw);
            String answer = response.path("answer").asText();
            if (!answer.isBlank()) return mapper.readTree(stripMarkdownFence(answer));
            return workflowResult(response);
        } catch (JsonProcessingException exception) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Stored extraction result is invalid", exception); }
    }

    private String stripMarkdownFence(String value) {
        String text = value == null ? "" : value.trim();
        if (!text.startsWith("```")) return text;
        int firstNewline = text.indexOf('\n');
        String content = firstNewline >= 0 ? text.substring(firstNewline + 1) : "";
        int end = content.lastIndexOf("```");
        if (end >= 0) content = content.substring(0, end);
        return content.trim();
    }

    public ContractModels.ContractDetail confirmExtraction(Long contractId, Map<String, Object> confirmation) {
        ContractModels.ContractDetail current = contracts.get(contractId);
        JsonNode extraction = latestExtraction(contractId);
        JsonNode basic = extraction.path("basic_info");
        if (confirmation == null || !Boolean.TRUE.equals(confirmation.get("confirm"))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Explicit confirmation is required");
        String name = selected(confirmation, "contractName", basic.path("contract_name").asText(null));
        String currency = normalizeCurrency(selected(confirmation, "currency", basic.path("currency").asText(null)));
        java.math.BigDecimal amount = decimalValue(confirmation.get("totalAmount"), basic.path("total_amount"));
        String signDate = validDate(selected(confirmation, "signDate", basic.path("sign_date").asText(null)));
        String effectiveDate = validDate(selected(confirmation, "effectiveDate", basic.path("effective_date").asText(null)));
        String expireDate = validDate(selected(confirmation, "expireDate", basic.path("expire_date").asText(null)));
        Integer direction = directionValue(selected(confirmation, "paymentDirection", basic.path("payment_direction").asText(null)));
        contracts.confirmIntakeType(contractId, longValue(confirmation.get("typeId")));
        contracts.confirmIntakeParty(contractId, selected(confirmation, "partyId", null));
        if (name != null && name.isBlank()) name = null;
        String now = Instant.now().toString();
        jdbc.update("UPDATE contract_draft SET name=COALESCE(?,name),total_amount=COALESCE(?,total_amount),currency=COALESCE(?,currency),payment_direction=COALESCE(?,payment_direction),sign_date=COALESCE(?,sign_date),effective_date=COALESCE(?,effective_date),expire_date=COALESCE(?,expire_date),updated_at=? WHERE id=?", name, amount, currency, direction, signDate, effectiveDate, expireDate, now, contractId);
        jdbc.update("INSERT INTO contract_operation_log(contract_id,action,description,operated_at) VALUES(?,?,?,?)", contractId, "AI_EXTRACT_CONFIRM", "Confirmed AI extraction fields after comparison", now);
        confirmFulfillment(contractId, confirmation, now);
        return contracts.get(current.id());
    }

    @SuppressWarnings("unchecked")
    private void confirmFulfillment(Long contractId, Map<String, Object> confirmation, String now) {
        Long taskId = latestExtractionTaskId(contractId);
        int paymentCount = 0;
        for (Map<String, Object> item : listOfMaps(confirmation.get("paymentPlans"))) {
            if (!Boolean.TRUE.equals(item.get("selected"))) continue;
            String name = text(item.get("planName"));
            BigDecimal amount = decimalValue(item.get("amount"), null);
            String dueDate = validDate(text(item.get("dueDate")));
            if (name == null || amount == null || amount.signum() <= 0 || dueDate == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已选付款计划必须包含名称、正数金额和标准日期");
            int phase = integer(item.get("phaseNo"), paymentCount + 1);
            String direction = "RECEIVE".equalsIgnoreCase(text(item.get("direction"))) ? "RECEIVE" : "PAY";
            int changed = jdbc.update("UPDATE contract_payment_plan SET item_name=?,direction=?,amount=?,due_date=?,remark=?,source_extract_task_id=?,is_ai_confirmed=1 WHERE contract_id=? AND source_extract_task_id=? AND phase_no=?", name, direction, amount, dueDate, text(item.get("triggerCondition")), taskId, contractId, taskId, phase);
            if (changed == 0) {
                long id = nextId("contract_payment_plan", 9000);
                jdbc.update("INSERT INTO contract_payment_plan(id,contract_id,item_name,direction,amount,due_date,status,remark,phase_no,trigger_condition,source_extract_task_id,is_ai_confirmed) VALUES(?,?,?,?,?,?,?,?,?,?,?,1)", id, contractId, name, direction, amount, dueDate, "PENDING", text(item.get("triggerCondition")), phase, text(item.get("triggerCondition")), taskId);
            }
            paymentCount++;
        }
        int milestoneCount = 0;
        for (Map<String, Object> item : listOfMaps(confirmation.get("fulfillmentItems"))) {
            if (!Boolean.TRUE.equals(item.get("selected"))) continue;
            String name = text(item.get("name"));
            String dueDate = validDate(text(item.get("dueDate")));
            if (name == null || dueDate == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "已选履行节点必须包含名称和标准日期");
            int phase = integer(item.get("phaseNo"), milestoneCount + 1);
            int changed = jdbc.update("UPDATE contract_fulfillment_milestone SET name=?,due_date=?,remark=?,source_extract_task_id=?,is_ai_confirmed=1 WHERE contract_id=? AND source_extract_task_id=? AND name=?", name, dueDate, text(item.get("description")), taskId, contractId, taskId, name);
            if (changed == 0) {
                long id = nextId("contract_fulfillment_milestone", 9500);
                jdbc.update("INSERT INTO contract_fulfillment_milestone(id,contract_id,name,due_date,status,remark,source_extract_task_id,is_ai_confirmed) VALUES(?,?,?,?,?,?,?,1)", id, contractId, name, dueDate, "PENDING", text(item.get("description")), taskId);
            }
            milestoneCount++;
        }
        if (paymentCount > 0 || milestoneCount > 0) jdbc.update("INSERT INTO contract_operation_log(contract_id,action,description,operated_at) VALUES(?,?,?,?)", contractId, "AI_FULFILLMENT_CONFIRM", "Confirmed " + paymentCount + " payment plans and " + milestoneCount + " fulfillment items", now);
    }

    public Map<String, Object> fulfillmentRecommendations(Long contractId) {
        contracts.get(contractId);
        JsonNode extraction = latestExtraction(contractId);
        return Map.of("paymentPlans", recommendations(extraction, "payment_plans"), "fulfillmentItems", recommendations(extraction, "fulfillment_items", "fulfillment_milestones"));
    }

    public Map<String, Object> revokeFulfillment(Long contractId) {
        contracts.get(contractId);
        Long taskId = latestExtractionTaskId(contractId);
        int payments = jdbc.update("UPDATE contract_payment_plan SET status='CANCELLED' WHERE contract_id=? AND source_extract_task_id=? AND is_ai_confirmed=1 AND status='PENDING'", contractId, taskId);
        int items = jdbc.update("UPDATE contract_fulfillment_milestone SET status='CANCELLED' WHERE contract_id=? AND source_extract_task_id=? AND is_ai_confirmed=1 AND status='PENDING'", contractId, taskId);
        if (payments == 0 && items == 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "当前解析任务没有可撤销的 AI 写入项");
        jdbc.update("INSERT INTO contract_operation_log(contract_id,action,description,operated_at) VALUES(?,?,?,?)", contractId, "AI_FULFILLMENT_REVOKE", "Revoked AI fulfillment entries: " + payments + " payment plans and " + items + " fulfillment items", Instant.now().toString());
        return Map.of("payments", payments, "fulfillmentItems", items);
    }

    private List<Map<String, Object>> recommendations(JsonNode node, String... names) {
        JsonNode array = null;
        for (String name : names) if (node.path(name).isArray()) { array = node.path(name); break; }
        List<Map<String, Object>> result = new ArrayList<>();
        if (array == null) return result;
        for (JsonNode item : array) {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("selected", false);
            row.put("phaseNo", item.path("phase_no").asInt(result.size() + 1));
            row.put("planName", item.path("plan_name").asText(item.path("name").asText("")));
            row.put("direction", paymentDirection(item));
            row.put("amount", item.path("amount").isNumber() ? item.path("amount").decimalValue() : item.path("plan_amount").isNumber() ? item.path("plan_amount").decimalValue() : null);
            row.put("dueDate", item.path("due_date").asText(item.path("plan_due_date").asText("")));
            row.put("triggerCondition", item.path("trigger_condition").asText(item.path("evidence").asText("")));
            row.put("name", item.path("name").asText(item.path("item_name").asText("")));
            row.put("description", item.path("item_description").asText(item.path("description").asText("")));
            result.add(row);
        }
        return result;
    }

    private Long latestExtractionTaskId(Long contractId) { return jdbc.queryForObject("SELECT id FROM ai_task WHERE contract_id=? AND task_type='EXTRACT' ORDER BY id DESC LIMIT 1", Long.class, contractId); }
    private List<Map<String, Object>> listOfMaps(Object value) { if (!(value instanceof List<?> list)) return List.of(); List<Map<String, Object>> result = new ArrayList<>(); for (Object item : list) if (item instanceof Map<?, ?> map) { Map<String, Object> row = new java.util.LinkedHashMap<>(); map.forEach((key, val) -> row.put(String.valueOf(key), val)); result.add(row); } return result; }
    private String text(Object value) { return value == null || value.toString().isBlank() ? null : value.toString().trim(); }
    private int integer(Object value, int fallback) { try { return value == null ? fallback : Integer.parseInt(value.toString()); } catch (RuntimeException e) { return fallback; } }
    private Long longValue(Object value) { try { return value == null || value.toString().isBlank() ? null : Long.parseLong(value.toString()); } catch (RuntimeException e) { return null; } }

    private String selected(Map<String, Object> values, String key, String fallback) { Object value = values.get(key); return value == null || value.toString().isBlank() ? fallback : value.toString().trim(); }
    private String paymentDirection(JsonNode item) { JsonNode value = item.has("direction") ? item.path("direction") : item.path("payment_direction"); if (value.isInt() || value.isLong()) return value.asInt() == 1 ? "RECEIVE" : "PAY"; String text = value.asText("PAY"); return "1".equals(text) || "RECEIVE".equalsIgnoreCase(text) || text.contains("收款") ? "RECEIVE" : "PAY"; }
    private String normalizeCurrency(String value) { if (value == null || value.isBlank()) return null; return value.contains("人民币") || value.equalsIgnoreCase("RMB") ? "CNY" : value.toUpperCase(); }
    private java.math.BigDecimal decimalValue(Object value, JsonNode fallback) { try { String text = value == null ? fallback == null ? null : fallback.asText(null) : value.toString(); return text == null || text.isBlank() || "null".equalsIgnoreCase(text) ? null : new java.math.BigDecimal(text.replace(",", "")); } catch (NumberFormatException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI amount is not a valid number"); } }
    private String validDate(String value) { if (value == null || value.isBlank()) return null; try { java.time.LocalDate.parse(value); return value; } catch (RuntimeException exception) { return null; } }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private Integer directionValue(String value) { if (value == null || value.isBlank()) return null; if (value.contains("支付") || value.contains("付款") || value.equals("PAY")) return 2; if (value.contains("收款") || value.contains("收取") || value.equals("RECEIVE")) return 1; return null; }

    public List<AiTask> tasks(Long contractId) {
        return jdbc.query("SELECT id,task_id,task_type,status,content_version_id,summary,created_at,finished_at FROM ai_task WHERE contract_id=? ORDER BY id DESC",
                (rs, row) -> new AiTask(rs.getLong("id"), rs.getString("task_id"), rs.getString("task_type"), rs.getString("status"),
                        rs.getLong("content_version_id"), rs.getString("summary"), Instant.parse(rs.getString("created_at")),
                        rs.getString("finished_at") == null ? null : Instant.parse(rs.getString("finished_at"))), contractId);
    }

    public AiReview review(Long contractId) {
        return jdbc.query("SELECT id,task_id,review_view,overall_level,summary,risks_json,confirmed,created_at FROM ai_review WHERE contract_id=? ORDER BY id DESC LIMIT 1",
                (rs, row) -> toReview(rs), contractId).stream().findFirst().orElse(null);
    }

    public AiReview confirmReview(Long contractId) {
        AiReview review = review(contractId);
        if (review == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "AI review does not exist");
        jdbc.update("UPDATE ai_review SET confirmed=1 WHERE id=?", review.id());
        jdbc.update("UPDATE ai_task SET status='CONFIRMED' WHERE id=?", review.taskId());
        return getReviewByTask(review.taskId());
    }

    public ContractModels.ApprovalInfo submitApproval(Long contractId) {
        contracts.ensureEditable(contractId);
        ContentVersion version = context(contractId).version();
        Instant now = Instant.now();
        Integer pending = jdbc.queryForObject("SELECT COUNT(*) FROM approval_mock WHERE contract_id=? AND status='PENDING'", Integer.class, contractId);
        if (pending != null && pending > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "当前合同已有审批中的流程");
        long id = nextId("approval_mock", 6000);
        int generation = jdbc.queryForObject("SELECT COALESCE(MAX(approval_generation),0)+1 FROM t_contract_approval_record WHERE contract_id=?", Integer.class, contractId);
        var ticket = approvalProvider.submit(String.valueOf(contractId), "Contract approval " + contractId,
                Map.of("contractId", contractId, "contentVersionId", version.id(), "contentHash", version.contentHash()));
        if (mysqlProfile()) jdbc.update("INSERT INTO approval_mock(id,contract_id,approval_no,status,result,content_version_id,content_hash,approval_generation,updated_at) VALUES(?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE approval_no=VALUES(approval_no),status=VALUES(status),result=VALUES(result),content_version_id=VALUES(content_version_id),content_hash=VALUES(content_hash),approval_generation=VALUES(approval_generation),updated_at=VALUES(updated_at)", id, contractId, ticket.externalId(), "PENDING", null, version.id(), version.contentHash(), generation, now.toString());
        else jdbc.update("INSERT INTO approval_mock(id,contract_id,approval_no,status,result,content_version_id,content_hash,approval_generation,updated_at) VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT(contract_id) DO UPDATE SET approval_no=excluded.approval_no,status=excluded.status,result=NULL,content_version_id=excluded.content_version_id,content_hash=excluded.content_hash,approval_generation=excluded.approval_generation,updated_at=excluded.updated_at", id, contractId, ticket.externalId(), "PENDING", null, version.id(), version.contentHash(), generation, now.toString());
        jdbc.update("UPDATE contract_draft SET approval_status=1,updated_at=? WHERE id=?", now.toString(), contractId);
        recordApproval(contractId, ticket.externalId(), version.id(), generation, 1, null, now);
        logApproval(contractId, "APPROVAL_SUBMIT", "提交审批第" + generation + "轮，冻结正文版本 v" + version.versionNo(), now);
        return approval(contractId);
    }

    public ContractModels.ApprovalInfo approve(Long contractId) {
        return resolveApproval(contractId, "APPROVED", null);
    }

    public ContractModels.ApprovalInfo reject(Long contractId, String opinion) {
        return resolveApproval(contractId, "REJECTED", opinion);
    }

    public ContractModels.ApprovalInfo withdraw(Long contractId, String opinion) {
        return resolveApproval(contractId, "WITHDRAWN", opinion);
    }

    private ContractModels.ApprovalInfo resolveApproval(Long contractId, String target, String opinion) {
        Instant now = Instant.now();
        int changed = jdbc.update("UPDATE approval_mock SET status=?,result=?,updated_at=? WHERE contract_id=? AND status='PENDING'", target, target, now.toString(), contractId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval does not exist");
        ApprovalSnapshot snapshot = jdbc.query("SELECT approval_no,content_version_id,approval_generation FROM approval_mock WHERE contract_id=?", (rs, row) -> new ApprovalSnapshot(rs.getString(1), rs.getLong(2), rs.getInt(3)), contractId).stream().findFirst().orElseThrow();
        int action = "APPROVED".equals(target) ? 2 : "REJECTED".equals(target) ? 3 : 4;
        String lifecycle = "APPROVED".equals(target) ? "待签署" : "草稿";
        jdbc.update("UPDATE contract_draft SET status=?,contract_status=?,approval_status=?,updated_at=? WHERE id=?", lifecycle, "APPROVED".equals(target) ? 1 : 0, "APPROVED".equals(target) ? 2 : "REJECTED".equals(target) ? 3 : 4, now.toString(), contractId);
        recordApproval(contractId, snapshot.approvalNo(), snapshot.contentVersionId(), snapshot.generation(), action, opinion, now);
        logApproval(contractId, "APPROVAL_" + target, "APPROVED".equals(target) ? "审批通过，合同进入待签署" : "审批" + ("REJECTED".equals(target) ? "驳回" : "撤回") + "，合同恢复草稿可编辑", now);
        return approval(contractId);
    }

    public ContractModels.ApprovalInfo approval(Long contractId) {
        return jdbc.query("SELECT id,approval_no,status,result,update_time FROM sys_approval_instance WHERE biz_type='contract' AND biz_id=?", (rs, row) -> new ContractModels.ApprovalInfo(rs.getLong("id"), rs.getString("approval_no"), approvalStatus(rs.getInt("status")), approvalResult((Integer) rs.getObject("result")), parseInstant(rs.getString("update_time"))), contractId).stream().findFirst().orElse(null);
    }

    private Instant parseInstant(String value) {
        if (value == null) return null;
        try { return Instant.parse(value); }
        catch (java.time.format.DateTimeParseException exception) { return java.sql.Timestamp.valueOf(value).toInstant(); }
    }

    private String approvalStatus(int status) { return switch (status) { case 2 -> "APPROVED"; case 3 -> "REJECTED"; case 4 -> "WITHDRAWN"; default -> "PENDING"; }; }
    private String approvalResult(Integer result) { return result == null ? null : result == 1 ? "APPROVED" : result == 2 ? "REJECTED" : result == 3 ? "WITHDRAWN" : null; }

    private void recordApproval(Long contractId, String approvalNo, Long contentVersionId, int generation, int action, String opinion, Instant now) {
        long id = nextId("t_contract_approval_record", 7000);
        if (mysqlProfile()) {
            jdbc.update("INSERT INTO t_contract_approval_record(id,contract_id,approval_no,contract_content_version_id,approval_generation,approval_action,approve_opinion,operate_time,record_status,create_by,create_time,deleted) VALUES(?,?,?,?,?,?,?,?,1,?,?,0)", id, contractId, approvalNo, contentVersionId, generation, action, opinion, formalTime(now), 10001L, formalTime(now));
        } else {
            jdbc.update("INSERT INTO t_contract_approval_record(id,contract_id,approval_no,contract_content_version_id,approval_generation,approval_action,approve_opinion,operate_time,record_status) VALUES(?,?,?,?,?,?,?,?,1)", id, contractId, approvalNo, contentVersionId, generation, action, opinion, now.toString());
        }
    }

    private String formalTime(Instant now) {
        return java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(java.time.ZoneOffset.UTC).format(now);
    }

    private void logApproval(Long contractId, String action, String description, Instant now) {
        jdbc.update("INSERT INTO contract_operation_log(contract_id,action,description,operated_at) VALUES(?,?,?,?)", contractId, action, description, now.toString());
    }

    private record ApprovalSnapshot(String approvalNo, Long contentVersionId, int generation) { }

    private Context context(Long contractId) {
        var contract = contracts.get(contractId);
        ContentVersion version = jdbc.query("SELECT id,version_no,source,content_hash,is_current,created_at FROM contract_content_version WHERE contract_id=? AND is_current=1", (rs, row) -> new ContentVersion(rs.getLong("id"), rs.getInt("version_no"), rs.getString("source"), rs.getString("content_hash"), true, Instant.parse(rs.getString("created_at"))), contractId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Save contract content before using AI"));
        String text = jdbc.queryForObject("SELECT content FROM contract_content_version WHERE id=?", String.class, version.id());
        return new Context(contract, version, text);
    }

    private Map<String, Object> extractInputs(Context context) {
        return Map.of("contract_text", context.text(), "contract_id", String.valueOf(context.contract().id()), "contract_type", context.contract().typeName(), "content_version_id", String.valueOf(context.version().id()), "schema_version", "extract-v1");
    }

    private Map<String, Object> reviewInputs(Context context, String view) {
        String parties = "Counterparty: " + context.contract().partyName() + "; relation: " + context.contract().relationType();
        return Map.of("contract_id", String.valueOf(context.contract().id()), "content_version_id", String.valueOf(context.version().id()), "contract_type", context.contract().typeName(), "our_role", "OUR_SIDE", "review_view", view, "confirmed_party_info", parties, "contract_text", context.text(), "schema_version", "review-v1", "query", "Review the contract risks from the specified view.");
    }

    private JsonNode call(String capability, Map<String, Object> inputs) {
        Long contractId = null; try { if (inputs.get("contract_id") != null) contractId = Long.valueOf(String.valueOf(inputs.get("contract_id"))); } catch (NumberFormatException ignored) { }
        Instant started = Instant.now();
        try { JsonNode response = aiProvider.submit(capability, inputs); logAiCall(contractId, capability, "SUCCESS", null, started); return response; }
        catch (RuntimeException exception) { logAiCall(contractId, capability, "FAILED", exception.getMessage(), started); throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI provider request failed: " + exception.getMessage(), exception); }
    }

    private void logAiCall(Long contractId, String capability, String status, String error, Instant createdAt) {
        try { jdbc.update("INSERT INTO ai_call_log(id,contract_id,capability,provider,model_or_workflow,status,error_summary,created_at) VALUES(?,?,?,?,?,?,?,?)", nextId("ai_call_log", 11000), contractId, capability, aiProvider.name(), null, status, error == null ? null : error.substring(0, Math.min(error.length(), 500)), createdAt.toString()); } catch (RuntimeException ignored) { }
    }

    private AiTask saveTask(Long contractId, ContentVersion version, String type, String status, String summary, JsonNode response) {
        long id = nextId("ai_task", 4000); Instant now = Instant.now();
        jdbc.update("INSERT INTO ai_task(id,contract_id,task_id,task_type,status,content_version_id,summary,provider,raw_response,created_at,finished_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)", id, contractId, UUID.randomUUID().toString(), type, status, version.id(), summary, aiProvider.name(), write(response), now.toString(), now.toString());
        return tasks(contractId).stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
    }

    private String extractionSummary(JsonNode response) {
        JsonNode answer = response.path("answer");
        if (!answer.isMissingNode() && !answer.asText().isBlank()) return answer.asText().length() > 240 ? answer.asText().substring(0, 240) : answer.asText();
        return "Dify extraction completed";
    }

    private ReviewResult parseReview(JsonNode response) {
        JsonNode result = workflowResult(response);
        if (!result.isObject()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Dify review response does not contain data.outputs.result");
        List<RiskItem> risks = new java.util.ArrayList<>();
        for (JsonNode item : result.path("risks")) risks.add(new RiskItem(item.path("category").asText("Uncategorized"), item.path("level").asText("UNKNOWN"), item.path("risk_point").asText(), item.path("basis").asText(), item.path("advice").asText(), false));
        String level = result.path("overall_risk_level").asText("UNKNOWN");
        String summary = result.path("review_summary").asText();
        if (summary.isBlank()) summary = "Dify review: " + level + ", risks: " + risks.size();
        return new ReviewResult(level, summary, risks);
    }

    private ReviewResult mockReview(ContentVersion version) { return new ReviewResult("MEDIUM", "Mock legal review for content version " + version.versionNo(), List.of(new RiskItem("PAYMENT", "MEDIUM", "Payment terms need review", "Mock result", "Confirm payment schedule and late-payment liability", false))); }
    private JsonNode workflowResult(JsonNode response) {
        JsonNode outputs = response.path("data").path("outputs");
        if (!outputs.isObject()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Dify workflow response does not contain data.outputs");
        JsonNode result = outputs.path("result");
        if (result.isObject()) return unwrapResult(result);
        if (result.isTextual()) {
            try { return unwrapResult(mapper.readTree(result.asText())); }
            catch (JsonProcessingException exception) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Dify workflow result is not valid JSON", exception); }
        }
        return unwrapResult(outputs);
    }
    private JsonNode unwrapResult(JsonNode result) { return result.path("result").isObject() ? result.path("result") : result; }
    private AiReview getReviewByTask(Long taskId) { return jdbc.query("SELECT id,task_id,review_view,overall_level,summary,risks_json,confirmed,created_at FROM ai_review WHERE task_id=?", (rs, row) -> toReview(rs), taskId).stream().findFirst().orElseThrow(); }
    private AiReview toReview(java.sql.ResultSet rs) throws java.sql.SQLException { return new AiReview(rs.getLong("id"), rs.getLong("task_id"), rs.getString("review_view"), rs.getString("overall_level"), rs.getString("summary"), read(rs.getString("risks_json")), rs.getInt("confirmed") == 1, Instant.parse(rs.getString("created_at"))); }
    private String write(Object value) { try { return mapper.writeValueAsString(value); } catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot serialize AI response", exception); } }
    private List<RiskItem> read(String value) { try { return mapper.readerForListOf(RiskItem.class).readValue(value); } catch (JsonProcessingException exception) { throw new IllegalStateException("Cannot read AI risk results", exception); } }
    private long nextId(String table, long fallback) { return jdbc.queryForObject("SELECT COALESCE(MAX(id),?)+1 FROM " + table, Long.class, fallback); }
    private void addColumn(String table, String column, String type) { try { jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type); } catch (RuntimeException ignored) { } }
    private boolean mysqlProfile() { String profiles = System.getenv("SPRING_PROFILES_ACTIVE"); return profiles != null && profiles.contains("mysql"); }
    private record Context(ContractModels.ContractDetail contract, ContentVersion version, String text) {}
    private record ReviewResult(String level, String summary, List<RiskItem> risks) {}
}
