package com.hrcontract.integration;

import java.util.Map;

/** Boundary for the enterprise approval engine. */
public interface ApprovalProvider {
    String name();
    ApprovalTicket submit(String businessKey, String title, Map<String, Object> payload);
    boolean testConnection();

    record ApprovalTicket(String externalId, String status) {}
}
