package com.hrcontract.application;

import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

/** Extracts readable text from locally stored office documents before AI processing. */
@Service
public class DocumentTextExtractionService {
    private static final int MAX_TEXT_LENGTH = 300_000;
    private final Tika tika = new Tika();

    public String extract(Path file, String fileName) {
        String extension = extension(fileName);
        if ("png".equals(extension) || "jpg".equals(extension) || "jpeg".equals(extension)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "扫描件已归档；OCR 能力尚未接入，暂不能自动解析图片内容");
        }
        try {
            String text;
            try (var input = Files.newInputStream(file)) {
                text = tika.parseToString(input);
            }
            if (text == null || text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "未能从文件中提取可解析文本，请检查文件或改为手工录入正文");
            }
            return text.length() > MAX_TEXT_LENGTH ? text.substring(0, MAX_TEXT_LENGTH).trim() : text.trim();
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "文件文本提取失败，请上传可读取的 Word 或 PDF 文件", exception);
        }
    }

    private String extension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(java.util.Locale.ROOT);
    }
}
