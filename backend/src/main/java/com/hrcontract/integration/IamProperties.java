package com.hrcontract.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IamProperties {
    @Value("${contract.auth.mode:local}") private String mode;
    @Value("${contract.auth.iam-base-url:}") private String baseUrl;
    @Value("${contract.auth.gateway-base-url:}") private String gatewayBaseUrl;
    @Value("${contract.auth.client-id:}") private String clientId;
    @Value("${contract.auth.client-secret:}") private String clientSecret;
    @Value("${contract.auth.app-code:}") private String appCode;
    @Value("${contract.auth.redirect-uri:http://localhost:19090/api/auth/oidc/callback}") private String redirectUri;
    @Value("${contract.auth.frontend-url:http://localhost:5173}") private String frontendUrl;
    @Value("${contract.auth.role-default:VIEWER}") private String roleDefault;
    @Value("${contract.auth.oidc-authorize-path:/oauth2/authorize}") private String authorizePath;
    @Value("${contract.auth.oidc-token-path:/oauth2/token}") private String tokenPath;
    public boolean iam() { return "iam".equalsIgnoreCase(mode); }
    public String mode() { return mode; } public String baseUrl() { return baseUrl; } public String gatewayBaseUrl() { return gatewayBaseUrl.isBlank() ? baseUrl : gatewayBaseUrl; } public String clientId() { return clientId; } public String clientSecret() { return clientSecret; } public String appCode() { return appCode; } public String redirectUri() { return redirectUri; } public String frontendUrl() { return frontendUrl; } public String roleDefault() { return roleDefault; } public String authorizePath() { return authorizePath; } public String tokenPath() { return tokenPath; }
    public boolean configured() { return !baseUrl.isBlank() && !clientId.isBlank() && !appCode.isBlank(); }
    public boolean oidcConfigured() { return configured() && !clientSecret.isBlank(); }
}
