package com.hrcontract.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@ConditionalOnProperty(name = "contract.ai.legacy-provider", havingValue = "true")
public class MockAiProvider implements AiProvider {
    private final ObjectMapper mapper;

    public MockAiProvider(ObjectMapper mapper) { this.mapper = mapper; }

    @Override public String name() { return "mock"; }

    @Override public JsonNode submit(String capability, Map<String, Object> inputs) {
        return mapper.createObjectNode()
                .put("provider", name())
                .put("capability", capability)
                .put("status", "completed");
    }

    @Override public boolean testConnection() { return true; }
}
