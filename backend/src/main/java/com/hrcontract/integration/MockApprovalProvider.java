package com.hrcontract.integration;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class MockApprovalProvider implements ApprovalProvider {
    @Override public String name() { return "mock-approval"; }
    @Override public ApprovalTicket submit(String businessKey, String title, Map<String, Object> payload) { return new ApprovalTicket("MOCK-" + UUID.randomUUID(), "PENDING"); }
    @Override public boolean testConnection() { return true; }
}
