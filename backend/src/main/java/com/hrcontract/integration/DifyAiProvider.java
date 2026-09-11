package com.hrcontract.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Contract extraction is a chat application; legal review is a Workflow application. Both use SSE. */
@ConditionalOnProperty(name = "contract.ai.legacy-provider", havingValue = "true")
public class DifyAiProvider implements AiProvider {
    private final AiProviderProperties properties;
    private final ObjectMapper mapper;

    public DifyAiProvider(AiProviderProperties properties, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override public String name() { return "dify"; }

    @Override
    public JsonNode submit(String capability, Map<String, Object> inputs) {
        boolean extraction = "extract".equals(capability);
        String key = extraction ? properties.getExtractApiKey() : properties.getReviewApiKey();
        if (key == null || key.isBlank()) throw new IllegalStateException("Dify API key is not configured for " + capability);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", inputs);
        if (extraction) body.put("query", "Please extract the contract and return the configured JSON schema.");
        body.put("response_mode", "streaming");
        body.put("user", "contract-system");
        return stream(extraction ? "/chat-messages" : "/workflows/run", key, body, extraction);
    }

    @Override
    public boolean testConnection() {
        return properties.getDifyBaseUrl() != null && !properties.getDifyBaseUrl().isBlank()
                && properties.getExtractApiKey() != null && !properties.getExtractApiKey().isBlank()
                && properties.getReviewApiKey() != null && !properties.getReviewApiKey().isBlank();
    }

    private JsonNode stream(String path, String key, Map<String, Object> body, boolean extraction) {
        try {
            HttpURLConnection connection = (HttpURLConnection) URI.create(trim(properties.getDifyBaseUrl()) + path).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(properties.getConnectTimeoutSeconds() * 1000);
            connection.setReadTimeout(properties.getReadTimeoutSeconds() * 1000);
            connection.setRequestProperty("Authorization", "Bearer " + key);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Accept", "text/event-stream");
            byte[] json = mapper.writeValueAsBytes(body);
            connection.getOutputStream().write(json);
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("Dify HTTP " + status + ": " + read(connection.getErrorStream()));
            StringBuilder answer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) continue;
                    JsonNode event = mapper.readTree(line.substring(5).trim());
                    String eventType = event.path("event").asText();
                    if ("message".equals(eventType)) answer.append(event.path("answer").asText());
                    if ("workflow_finished".equals(eventType)) return event;
                    if (extraction && "message_end".equals(eventType)) {
                        ObjectNode result = mapper.createObjectNode();
                        result.put("answer", answer.toString());
                        result.set("message_end", event);
                        return result;
                    }
                    if ("error".equals(eventType)) throw new IllegalStateException("Dify stream error: " + event.path("message").asText("unknown error"));
                }
            }
            throw new IllegalStateException("Dify stream ended before a completion event");
        } catch (Exception exception) {
            throw new IllegalStateException("Dify request failed: " + exception.getMessage(), exception);
        }
    }

    private static String read(java.io.InputStream stream) {
        if (stream == null) return "";
        try (stream) { return new String(stream.readAllBytes(), StandardCharsets.UTF_8); }
        catch (Exception ignored) { return ""; }
    }
    private static String trim(String value) { return value == null ? "" : value.replaceAll("/+$", ""); }
}
