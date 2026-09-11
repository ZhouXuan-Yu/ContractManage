package com.hrcontract.integration;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockDirectoryProvider implements DirectoryProvider {
    @Override public String name() { return "mock-directory"; }
    @Override public List<DirectoryUser> users() {
        return List.of(new DirectoryUser("10001", "Local Administrator", "ADMIN", "100", "101"),
                new DirectoryUser("10002", "Business User", "BUSINESS", "100", "101"),
                new DirectoryUser("10003", "Legal User", "LEGAL", "100", "102"));
    }
    @Override public boolean testConnection() { return true; }
}
