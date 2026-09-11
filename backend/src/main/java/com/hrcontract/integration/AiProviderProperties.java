package com.hrcontract.integration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "contract.ai")
public class AiProviderProperties {
    private String provider = "mock";
    private String difyBaseUrl = "";
    private String extractApiKey = "";
    private String reviewApiKey = "";
    private int connectTimeoutSeconds = 10;
    private int readTimeoutSeconds = 300;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getDifyBaseUrl() { return difyBaseUrl; }
    public void setDifyBaseUrl(String difyBaseUrl) { this.difyBaseUrl = difyBaseUrl; }
    public String getExtractApiKey() { return extractApiKey; }
    public void setExtractApiKey(String extractApiKey) { this.extractApiKey = extractApiKey; }
    public String getReviewApiKey() { return reviewApiKey; }
    public void setReviewApiKey(String reviewApiKey) { this.reviewApiKey = reviewApiKey; }
    public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
    public int getReadTimeoutSeconds() { return readTimeoutSeconds; }
    public void setReadTimeoutSeconds(int readTimeoutSeconds) { this.readTimeoutSeconds = readTimeoutSeconds; }
}
