package com.hrcontract.integration;

/** Boundary for contract attachment storage. Contract business code persists only provider metadata. */
public interface FileStorageProvider {
    String name();
    String mode();
    StoredFile store(String fileName, byte[] content, String contentType);
    boolean testConnection();

    record StoredFile(String reference, String storagePath, String fileName, long size) {}
}
