package com.hrcontract.application;

import com.hrcontract.integration.DirectoryProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class IdentityDirectoryService {
    private final DirectoryProvider directory;
    public IdentityDirectoryService(DirectoryProvider directory) { this.directory = directory; }

    public Map<String, Object> contract() {
        return Map.of(
                "provider", directory.name(),
                "mode", "LOCAL_FALLBACK",
                "available", directory.testConnection(),
                "authoritative", false,
                "requiredFields", List.of("userId", "loginName", "displayName", "enabled", "orgId", "departmentId", "roleCodes"),
                "operations", List.of("current-user", "user-query", "organization-query", "department-query", "account-disable-notification"),
                "fallback", "HR/IAM unavailable or unconfigured: retain local session identity, show external directory unavailable, and do not overwrite local contract ownership.",
                "writeBoundary", "Contract system reads identity and organization data only; it does not create or modify HR/IAM master data.");
    }

    public List<DirectoryProvider.DirectoryUser> users() { return directory.users(); }
}
