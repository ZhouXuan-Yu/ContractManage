package com.hrcontract.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hrcontract.application.AiConfigService;
import com.hrcontract.application.ContractModels.AiConfig;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Server-side AI proxy. Business code never calls Dify or a model vendor directly. */
@Component
public class ConfigurableAiProvider implements AiProvider {
    private final AiConfigService configService; private final ObjectMapper mapper;
    public ConfigurableAiProvider(AiConfigService configService, ObjectMapper mapper) { this.configService = configService; this.mapper = mapper; }
    @Override public String name() { return configService.get().provider(); }

    @Override public JsonNode submit(String capability, Map<String, Object> inputs) {
        AiConfig config = configService.get(); validate(config, capability);
        if ("mock".equalsIgnoreCase(config.provider())) return mock(capability, inputs);
        if ("dify".equalsIgnoreCase(config.provider())) return callDify(config, capability, inputs);
        if ("deepseek".equalsIgnoreCase(config.provider()) || "company_ai".equalsIgnoreCase(config.provider()) || "gateway".equalsIgnoreCase(config.provider())) return callChat(config, capability, inputs);
        throw new IllegalStateException("不支持的 AI 提供方: " + config.provider());
    }

    @Override public boolean testConnection() { try { testConnectionOrThrow(); return true; } catch (RuntimeException exception) { return false; } }
    @Override public void testConnectionOrThrow() {
        try {
            AiConfig config = configService.get();
            if (!config.enabled()) throw new IllegalStateException("AI 能力未启用");
            if ("mock".equalsIgnoreCase(config.provider())) return;
            validateBase(config);
            if ("dify".equalsIgnoreCase(config.provider())) {
                String workflow = firstWorkflow(config);
                if (blank(workflow)) throw new IllegalStateException("Dify 至少需要配置一个工作流 ID");
                request(config, "/workflows/run", Map.of("inputs", Map.of("health_check", true), "response_mode", "blocking", "user", "contract-system", "workflow_id", workflow));
            } else {
                HttpURLConnection connection = connection(config, "/models", "GET");
                int status = connection.getResponseCode();
                if (status < 200 || status >= 300) throw new IllegalStateException("AI HTTP " + status + ": " + read(connection.getErrorStream()));
            }
        } catch (IllegalStateException exception) { throw exception; }
        catch (Exception exception) { throw new IllegalStateException("AI 连接测试失败: " + exception.getMessage(), exception); }
    }

    private void validate(AiConfig config, String capability) {
        if (!config.enabled()) throw new IllegalStateException("AI 能力未启用");
        boolean capabilityEnabled = switch (capability) { case "draft" -> config.draftEnabled(); case "extract" -> config.extractEnabled(); case "review" -> config.reviewEnabled(); case "fulfillment" -> config.fulfillmentEnabled(); default -> false; };
        if (!capabilityEnabled) throw new IllegalStateException("该 AI 能力未启用");
        if ("mock".equalsIgnoreCase(config.provider())) return;
        validateBase(config);
        if ("dify".equalsIgnoreCase(config.provider()) && blank(workflowId(config, capability))) throw new IllegalStateException("未配置 " + capability + " 工作流 ID");
    }
    private void validateBase(AiConfig config) { if (blank(config.baseUrl()) || !config.secretConfigured()) throw new IllegalStateException("AI 地址或 API Key 未配置"); }

    private JsonNode callDify(AiConfig config, String capability, Map<String, Object> inputs) {
        Map<String, Object> body = new LinkedHashMap<>(); body.put("inputs", inputs); body.put("response_mode", "blocking"); body.put("user", "contract-system"); body.put("workflow_id", workflowId(config, capability));
        return request(config, "/workflows/run", body);
    }

    private JsonNode callChat(AiConfig config, String capability, Map<String, Object> inputs) {
        String instruction = switch (capability) {
            case "draft" -> "根据输入起草合同草稿。返回 JSON：content（合同正文）、variables（变量数组）、warnings（风险提示数组）。只能输出 JSON。";
            case "extract" -> "从合同文本提取结构化信息。返回 JSON，字段包括 basic_info、payment_plans、fulfillment_items。只能输出 JSON。";
            case "review" -> "审查合同法律风险。返回 JSON：overall_risk_level、review_summary、risks（每项含 category、level、risk_point、basis、advice）。只能输出 JSON。";
            default -> "从合同中提取履约节点和收付款计划。返回 JSON。只能输出 JSON。";
        };
        Map<String, Object> message = new LinkedHashMap<>(); message.put("role", "user"); message.put("content", instruction + "\n输入：" + write(inputs));
        Map<String, Object> body = new LinkedHashMap<>(); body.put("model", blank(config.model()) ? "deepseek-chat" : config.model()); body.put("messages", java.util.List.of(Map.of("role", "system", "content", "你是企业合同管理系统的 AI 助手，结果必须由人工确认。"), message)); body.put("temperature", 0.2); body.put("response_format", Map.of("type", "json_object"));
        JsonNode response = request(config, "/chat/completions", body); String answer = response.path("choices").path(0).path("message").path("content").asText();
        if (blank(answer)) throw new IllegalStateException("AI 未返回有效内容");
        if ("extract".equals(capability) || "draft".equals(capability)) return mapper.createObjectNode().put("answer", answer);
        ObjectNode normalized = mapper.createObjectNode(); ObjectNode data = normalized.putObject("data"); ObjectNode outputs = data.putObject("outputs"); try { outputs.set("result", mapper.readTree(answer)); } catch (Exception exception) { throw new IllegalStateException("AI 返回的 JSON 无法解析", exception); } return normalized;
    }

    private JsonNode request(AiConfig config, String path, Map<String, Object> body) {
        try { HttpURLConnection connection = connection(config, path, "POST"); connection.setDoOutput(true); connection.getOutputStream().write(mapper.writeValueAsBytes(body)); int status = connection.getResponseCode(); if (status < 200 || status >= 300) throw new IllegalStateException("AI HTTP " + status + ": " + read(connection.getErrorStream())); return mapper.readTree(read(connection.getInputStream())); }
        catch (RuntimeException exception) { throw exception; } catch (Exception exception) { throw new IllegalStateException("AI 调用失败: " + exception.getMessage(), exception); }
    }
    private HttpURLConnection connection(AiConfig config, String path, String method) throws Exception { HttpURLConnection connection = (HttpURLConnection) URI.create(trim(config.baseUrl()) + path).toURL().openConnection(); connection.setRequestMethod(method); connection.setConnectTimeout(10000); connection.setReadTimeout(300000); connection.setRequestProperty("Authorization", "Bearer " + configService.secret()); connection.setRequestProperty("Content-Type", "application/json; charset=utf-8"); connection.setRequestProperty("Accept", "application/json"); return connection; }
    private JsonNode mock(String capability, Map<String, Object> inputs) { ObjectNode result = mapper.createObjectNode().put("provider", "mock").put("capability", capability).put("status", "completed"); if ("draft".equals(capability)) result.put("answer", write(Map.of("content", "合同草稿\n\n一、双方根据约定履行本合同。\n二、具体权利义务以双方确认文本为准。", "variables", java.util.List.of(), "warnings", java.util.List.of("当前为 Mock 结果，请配置真实 AI 后使用")))); return result; }
    private String workflowId(AiConfig c, String capability) { return switch (capability) { case "draft" -> c.draftWorkflowId(); case "extract" -> c.extractWorkflowId(); case "review" -> c.reviewWorkflowId(); case "fulfillment" -> c.fulfillmentWorkflowId(); default -> null; }; }
    private String firstWorkflow(AiConfig c) { return java.util.stream.Stream.of(c.draftWorkflowId(), c.extractWorkflowId(), c.reviewWorkflowId(), c.fulfillmentWorkflowId()).filter(v -> !blank(v)).findFirst().orElse(null); }
    private String write(Object value) { try { return mapper.writeValueAsString(value); } catch (Exception exception) { throw new IllegalStateException("AI request serialization failed", exception); } }
    private String read(InputStream stream) throws java.io.IOException { return stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8); }
    private String trim(String value) { return value == null ? "" : value.replaceAll("/+$", ""); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
