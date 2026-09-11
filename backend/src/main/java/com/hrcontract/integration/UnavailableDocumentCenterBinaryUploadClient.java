package com.hrcontract.integration;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Replaced when the platform supplies the binary-upload endpoint that returns fileId. */
@Component
public class UnavailableDocumentCenterBinaryUploadClient implements DocumentCenterBinaryUploadClient {
    @Override
    public UploadedBinary upload(String fileName, byte[] content, String contentType) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "文件中心二进制上传接口尚未配置，无法取得 fileId 后登记合同文档");
    }

    @Override public boolean available() { return false; }
}
