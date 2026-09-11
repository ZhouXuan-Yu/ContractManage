package com.hrcontract.integration;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/** Provider boundary. Domain services do not depend on a vendor response model. */
public interface AiProvider {
    String name();

    JsonNode submit(String capability, Map<String, Object> inputs);

    boolean testConnection();

    default void testConnectionOrThrow() {
        if (!testConnection()) throw new IllegalStateException("AI provider connection test failed");
    }
}
