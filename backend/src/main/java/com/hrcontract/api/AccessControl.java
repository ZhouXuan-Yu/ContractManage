package com.hrcontract.api;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Component
public class AccessControl {
    private static final Map<String, Set<String>> ROLE_PERMISSIONS = Map.of(
            "ADMIN", Set.of("VIEW", "EDIT", "APPROVE", "FULFILL", "TEMPLATE"),
            "LEGAL", Set.of("VIEW", "EDIT", "APPROVE", "TEMPLATE"),
            "BUSINESS", Set.of("VIEW", "EDIT", "FULFILL"),
            "VIEWER", Set.of("VIEW")
    );
    private final com.hrcontract.application.PermissionAuditService permissions;
    private final com.hrcontract.integration.IamProperties iamProperties;

    public AccessControl(com.hrcontract.application.PermissionAuditService permissions, com.hrcontract.integration.IamProperties iamProperties) { this.permissions = permissions; this.iamProperties = iamProperties; }

    public String currentRole(HttpServletRequest request) {
        Object sessionRole = request.getSession(false) == null ? null : request.getSession(false).getAttribute("role");
        if (sessionRole != null) return sessionRole.toString().toUpperCase();
        if (iamProperties.iam()) return "ANONYMOUS";
        String role = request.getHeader("X-Role");
        return role == null || role.isBlank() ? "ADMIN" : role.trim().toUpperCase();
    }

    public void require(HttpServletRequest request, String permission) {
        String role = currentRole(request);
        if (!permissionsFor(role).contains(permission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Permission denied: " + permission);
        }
    }

    public Map<String, Object> currentUser(HttpServletRequest request) {
        String role = currentRole(request);
        var session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) return Map.of("userId", session.getAttribute("userId"), "orgId", session.getAttribute("orgId"), "departmentId", session.getAttribute("departmentId"), "displayName", session.getAttribute("displayName"), "role", role, "permissions", permissionsFor(role));
        if (iamProperties.iam()) return Map.of("userId", "", "orgId", "", "departmentId", "", "displayName", "Anonymous", "role", role, "permissions", Set.of());
        String userId = request.getHeader("X-User-Id");
        String orgId = request.getHeader("X-Org-Id");
        String departmentId = request.getHeader("X-Department-Id");
        return Map.of("userId", userId == null || userId.isBlank() ? "10001" : userId,
                "orgId", orgId == null || orgId.isBlank() ? "100" : orgId,
                "departmentId", departmentId == null || departmentId.isBlank() ? "101" : departmentId,
                "displayName", "Local User", "role", role,
                "permissions", permissionsFor(role));
    }

    private Set<String> permissionsFor(String role) {
        try { return Set.copyOf(permissions.role(role).permissions()); }
        catch (RuntimeException ignored) { return ROLE_PERMISSIONS.getOrDefault(role, Set.of()); }
    }
}
