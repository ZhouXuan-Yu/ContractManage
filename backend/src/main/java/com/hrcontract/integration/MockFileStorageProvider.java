package com.hrcontract.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("mock-files")
public class MockFileStorageProvider implements FileStorageProvider {
    @Override public String name() { return "mock-file-storage"; }
    @Override public String mode() { return "MOCK"; }
    @Override public StoredFile store(String fileName, byte[] content, String contentType) { return new StoredFile("mock://files/" + UUID.randomUUID(), "", fileName, content.length); }
    @Override public boolean testConnection() { return true; }
}
