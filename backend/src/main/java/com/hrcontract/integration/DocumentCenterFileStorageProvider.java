package com.hrcontract.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Registers a binary already accepted by the company file service into the contract document library. */
@Component
@ConditionalOnProperty(prefix = "contract.document-center", name = "provider", havingValue = "document")
public class DocumentCenterFileStorageProvider implements FileStorageProvider {
    private final DocumentCenterProperties properties;
    private final DocumentCenterBinaryUploadClient binaryUpload;
    private final ObjectMapper mapper;

    public DocumentCenterFileStorageProvider(DocumentCenterProperties properties, DocumentCenterBinaryUploadClient binaryUpload, ObjectMapper mapper) {
        this.properties = properties;
        this.binaryUpload = binaryUpload;
        this.mapper = mapper;
    }

    @Override public String name() { return "iam-document-center"; }
    @Override public String mode() { return "DOCUMENT_CENTER"; }

    @Override
    public StoredFile store(String fileName, byte[] content, String contentType) {
        if (!properties.configured()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "文件中心网关、文档库或应用编码尚未配置");
        if (properties.serviceToken().isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "文件中心服务令牌尚未配置");
        DocumentCenterBinaryUploadClient.UploadedBinary uploaded = binaryUpload.upload(fileName, content, contentType);
        long itemId = register(uploaded);
        return new StoredFile("doc:" + properties.libraryId() + ":" + itemId, "", uploaded.fileName(), uploaded.size());
    }

    @Override public boolean testConnection() { return properties.configured() && !properties.serviceToken().isBlank() && binaryUpload.available(); }

    private long register(DocumentCenterBinaryUploadClient.UploadedBinary uploaded) {
        try {
            String url = trim(properties.gatewayBaseUrl()) + "/api/v1/docs/libraries/" + properties.libraryId() + "/files";
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + properties.serviceToken());
            connection.setRequestProperty("X-App-Code", properties.appCode());
            connection.setRequestProperty("Content-Type", "application/json");
            String body = mapper.writeValueAsString(Map.of("fileId", uploaded.fileId(), "itemName", uploaded.fileName()));
            connection.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            int status = connection.getResponseCode();
            String response = new String((status >= 400 ? connection.getErrorStream() : connection.getInputStream()).readAllBytes(), StandardCharsets.UTF_8);
            if (status < 200 || status >= 300) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "文件中心登记合同文档失败: HTTP " + status);
            JsonNode root = mapper.readTree(response);
            if (root.has("success") && !root.path("success").asBoolean()) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "文件中心登记合同文档失败: " + root.path("message").asText());
            JsonNode data = root.path("data");
            long itemId = data.path("id").asLong(data.path("itemId").asLong(0));
            if (itemId <= 0) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "文件中心登记响应缺少文档项 ID");
            return itemId;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "文件中心登记合同文档失败", exception);
        }
    }

    private String trim(String value) { return value.replaceAll("/+$", ""); }
}
