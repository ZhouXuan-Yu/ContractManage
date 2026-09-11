package com.hrcontract.application;

import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AiSecretCrypto {
    private static final String PREFIX = "ENC[v1]:";
    private final SecureRandom random = new SecureRandom();
    public String encrypt(String value) {
        if (value == null || value.isBlank()) return null;
        if (value.startsWith(PREFIX)) return value;
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length); System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception exception) { throw new IllegalStateException("Unable to encrypt AI credential", exception); }
    }
    public String decrypt(String value) {
        if (value == null || value.isBlank()) return null;
        if (!value.startsWith(PREFIX)) return value;
        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, payload, 0, 12));
            return new String(cipher.doFinal(payload, 12, payload.length - 12), StandardCharsets.UTF_8);
        } catch (Exception exception) { throw new IllegalStateException("Unable to decrypt AI credential", exception); }
    }
    private SecretKeySpec key() throws Exception {
        String configured = System.getenv().getOrDefault("CONTRACT_AI_CONFIG_KEY", "contract-development-key-change-in-production");
        return new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(configured.getBytes(StandardCharsets.UTF_8)), "AES");
    }
}
