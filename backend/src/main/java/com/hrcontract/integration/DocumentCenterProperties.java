package com.hrcontract.integration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DocumentCenterProperties {
    @Value("${contract.document-center.gateway-base-url:}") private String gatewayBaseUrl;
    @Value("${contract.document-center.library-id:}") private String libraryId;
    @Value("${contract.document-center.app-code:}") private String appCode;
    @Value("${contract.document-center.provider:local}") private String provider;
    @Value("${contract.document-center.service-token:}") private String serviceToken;

    public boolean configured() { return !gatewayBaseUrl.isBlank() && !libraryId.isBlank() && !appCode.isBlank(); }
    public String gatewayBaseUrl() { return gatewayBaseUrl; }
    public String libraryId() { return libraryId; }
    public String appCode() { return appCode; }
    public String provider() { return provider; }
    public String serviceToken() { return serviceToken; }
}
