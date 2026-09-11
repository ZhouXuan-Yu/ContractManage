package com.hrcontract.integration;

import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Local development fallback. Production storage is selected only after a document-center upload contract is supplied. */
@Component
@Profile("!mock-files")
@ConditionalOnProperty(prefix = "contract.document-center", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageProvider implements FileStorageProvider {
    @Override public String name() { return "local-file-storage"; }
    @Override public String mode() { return "LOCAL_FALLBACK"; }

    @Override
    public StoredFile store(String fileName, byte[] content, String contentType) {
        if (content == null || content.length == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件内容不能为空");
        try {
            Path directory = Path.of("data", "files");
            Files.createDirectories(directory);
            String reference = UUID.randomUUID().toString();
            String safeName = Path.of(fileName == null || fileName.isBlank() ? "upload.bin" : fileName).getFileName().toString();
            Path target = directory.resolve(reference + "-" + safeName);
            Files.copy(new java.io.ByteArrayInputStream(content), target, StandardCopyOption.REPLACE_EXISTING);
            return new StoredFile(reference, target.toString(), safeName, content.length);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "本地文件保存失败", exception);
        }
    }

    @Override public boolean testConnection() { return true; }
}
