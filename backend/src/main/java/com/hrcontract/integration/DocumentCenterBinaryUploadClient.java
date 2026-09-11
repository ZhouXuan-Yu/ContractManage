package com.hrcontract.integration;

/**
 * Platform-specific binary upload boundary. The published document-center API starts
 * at document registration and requires a fileId, so this is intentionally separate.
 */
public interface DocumentCenterBinaryUploadClient {
    UploadedBinary upload(String fileName, byte[] content, String contentType);
    boolean available();

    record UploadedBinary(long fileId, String fileName, long size) {}
}
