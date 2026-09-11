package com.hrcontract.integration;

import java.util.List;

/**
 * Boundary for HR/IAM identities. A real provider must supply stable user IDs,
 * account status, organization and department identifiers; the contract system
 * never becomes the authoritative organization directory.
 */
public interface DirectoryProvider {
    String name();
    List<DirectoryUser> users();
    boolean testConnection();

    record DirectoryUser(String id, String name, String role, String orgId, String departmentId) {}
}
