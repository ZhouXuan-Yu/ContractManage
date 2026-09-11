package com.hrcontract.application;

import com.hrcontract.application.ContractModels.AiConfig;
import com.hrcontract.application.ContractModels.UpdateAiConfigRequest;
import com.hrcontract.application.ContractModels.AiCapabilityTestResult;
import com.hrcontract.integration.AiProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;

@Service
public class AiConfigService {
    private final JdbcTemplate jdbc; private final AiSecretCrypto crypto; private final ObjectProvider<AiProvider> provider;
    public AiConfigService(JdbcTemplate jdbc, AiSecretCrypto crypto, ObjectProvider<AiProvider> provider) { this.jdbc = jdbc; this.crypto = crypto; this.provider = provider; }
    @PostConstruct @Profile("!mysql")
    void initialize() {
        jdbc.execute("CREATE TABLE IF NOT EXISTS contract_ai_config (id INTEGER PRIMARY KEY, config_name TEXT NOT NULL, provider TEXT NOT NULL, base_url TEXT, model TEXT, secret TEXT, draft_workflow_id TEXT, extract_workflow_id TEXT, review_workflow_id TEXT, fulfillment_workflow_id TEXT, enabled INTEGER NOT NULL DEFAULT 0, draft_enabled INTEGER NOT NULL DEFAULT 1, extract_enabled INTEGER NOT NULL DEFAULT 1, review_enabled INTEGER NOT NULL DEFAULT 1, fulfillment_enabled INTEGER NOT NULL DEFAULT 1, last_test_at TEXT, last_error TEXT, updated_at TEXT NOT NULL)");
        addColumn("contract_ai_config", "draft_enabled", "INTEGER NOT NULL DEFAULT 1"); addColumn("contract_ai_config", "extract_enabled", "INTEGER NOT NULL DEFAULT 1"); addColumn("contract_ai_config", "review_enabled", "INTEGER NOT NULL DEFAULT 1"); addColumn("contract_ai_config", "fulfillment_enabled", "INTEGER NOT NULL DEFAULT 1");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM contract_ai_config", Integer.class) == 0) jdbc.update("INSERT INTO contract_ai_config(id,config_name,provider,base_url,model,enabled,updated_at) VALUES(1,?,?,?,?,0,?)", "合同 AI 服务", "mock", "", "", Instant.now().toString());
    }
    public AiConfig get() { return jdbc.queryForObject("SELECT config_name,provider,base_url,model,secret,draft_workflow_id,extract_workflow_id,review_workflow_id,fulfillment_workflow_id,enabled,draft_enabled,extract_enabled,review_enabled,fulfillment_enabled,last_test_at,last_error FROM contract_ai_config WHERE id=1", (rs, row) -> new AiConfig(rs.getString("config_name"), rs.getString("provider"), rs.getString("base_url"), rs.getString("model"), rs.getString("secret") != null && !rs.getString("secret").isBlank(), rs.getString("draft_workflow_id"), rs.getString("extract_workflow_id"), rs.getString("review_workflow_id"), rs.getString("fulfillment_workflow_id"), rs.getInt("enabled") == 1, rs.getInt("draft_enabled") == 1, rs.getInt("extract_enabled") == 1, rs.getInt("review_enabled") == 1, rs.getInt("fulfillment_enabled") == 1, rs.getString("provider"), "数据库配置", rs.getString("last_test_at"), rs.getString("last_error"))); }
    public String secret() { return crypto.decrypt(jdbc.queryForObject("SELECT secret FROM contract_ai_config WHERE id=1", String.class)); }
    public AiConfig update(UpdateAiConfigRequest request) {
        if (request == null || blank(request.name()) || blank(request.provider())) throw bad("请填写 AI 服务名称和提供方");
        String secret = blank(request.secret()) ? null : crypto.encrypt(request.secret().trim());
        String sql = secret == null ? "UPDATE contract_ai_config SET config_name=?,provider=?,base_url=?,model=?,draft_workflow_id=?,extract_workflow_id=?,review_workflow_id=?,fulfillment_workflow_id=?,enabled=?,draft_enabled=?,extract_enabled=?,review_enabled=?,fulfillment_enabled=?,updated_at=? WHERE id=1" : "UPDATE contract_ai_config SET config_name=?,provider=?,base_url=?,model=?,secret=?,draft_workflow_id=?,extract_workflow_id=?,review_workflow_id=?,fulfillment_workflow_id=?,enabled=?,draft_enabled=?,extract_enabled=?,review_enabled=?,fulfillment_enabled=?,updated_at=? WHERE id=1";
        Object[] args = secret == null ? new Object[]{request.name().trim(), request.provider().trim(), request.baseUrl(), request.model(), request.draftWorkflowId(), request.extractWorkflowId(), request.reviewWorkflowId(), request.fulfillmentWorkflowId(), request.enabled() ? 1 : 0, request.draftEnabled() ? 1 : 0, request.extractEnabled() ? 1 : 0, request.reviewEnabled() ? 1 : 0, request.fulfillmentEnabled() ? 1 : 0, Instant.now().toString()} : new Object[]{request.name().trim(), request.provider().trim(), request.baseUrl(), request.model(), secret, request.draftWorkflowId(), request.extractWorkflowId(), request.reviewWorkflowId(), request.fulfillmentWorkflowId(), request.enabled() ? 1 : 0, request.draftEnabled() ? 1 : 0, request.extractEnabled() ? 1 : 0, request.reviewEnabled() ? 1 : 0, request.fulfillmentEnabled() ? 1 : 0, Instant.now().toString()};
        jdbc.update(sql, args); return get();
    }
    public AiConfig test() {
        AiConfig current = get(); String error = null;
        try { if (!current.enabled()) throw new IllegalStateException("AI 能力未启用"); if (!"mock".equalsIgnoreCase(current.provider())) provider.getObject().testConnectionOrThrow(); }
        catch (RuntimeException exception) { error = shorten(exception.getMessage()); }
        jdbc.update("UPDATE contract_ai_config SET last_test_at=?,last_error=? WHERE id=1", Instant.now().toString(), error); return get();
    }

    public AiCapabilityTestResult testCapability(String capability) {
        if (!java.util.Set.of("draft", "extract", "review", "fulfillment").contains(capability)) throw bad("不支持的 AI 能力");
        AiConfig current = get(); String message; boolean success;
        try {
            if (!current.enabled()) throw new IllegalStateException("AI 能力未启用");
            if (!capabilityEnabled(current, capability)) throw new IllegalStateException("该 AI 能力未启用");
            java.util.Map<String, Object> sample = new java.util.LinkedHashMap<>();
            sample.put("contract_id", "health-check"); sample.put("contract_type", "服务合同"); sample.put("contract_text", "甲乙双方就服务事项达成如下约定。合同金额为人民币10000元，期限为2026-01-01至2026-12-31。");
            sample.put("counterparty_name", "测试主体"); sample.put("our_party_name", "本组织"); sample.put("contract_name", "AI 能力测试合同"); sample.put("requirements", "仅用于接口连通性测试");
            provider.getObject().submit(capability, sample);
            success = true; message = "能力调用成功";
        } catch (RuntimeException exception) { success = false; message = exception.getMessage() == null ? "能力调用失败" : exception.getMessage(); }
        return new AiCapabilityTestResult(capability, current.provider(), success, message, Instant.now().toString());
    }
    private String shorten(String value) { return value == null ? "连接测试失败" : value.length() > 500 ? value.substring(0, 500) : value; }
    private boolean capabilityEnabled(AiConfig config, String capability) { return switch (capability) { case "draft" -> config.draftEnabled(); case "extract" -> config.extractEnabled(); case "review" -> config.reviewEnabled(); case "fulfillment" -> config.fulfillmentEnabled(); default -> false; }; }
    private void addColumn(String table, String column, String type) { try { jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type); } catch (RuntimeException ignored) { } }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
