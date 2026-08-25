package com.window.util;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@Slf4j
public class AesUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ENC_PREFIX = "ENC:";

    @Value("${app.encryption.secret:}")
    private String secretKeyBase64;

    private SecretKey key;

    @PostConstruct
    public void init() {
        if (secretKeyBase64 == null || secretKeyBase64.isBlank()) {
            throw new IllegalStateException(
                    "app.encryption.secret 未配置！请在 application.yml 或环境变量 AES_KEY 中设置 Base64 编码的 32 字节密钥");
        }
        byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "app.encryption.secret 解码后必须为 32 字节，当前: " + keyBytes.length + " 字节");
        }
        key = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return ENC_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        if (ciphertext.startsWith(ENC_PREFIX)) {
            return doDecrypt(ciphertext.substring(ENC_PREFIX.length()));
        }
        // Legacy plaintext fallback
        log.warn("Legacy plaintext API key detected — please re-save AI settings to encrypt");
        return ciphertext;
    }

    private String doDecrypt(String base64Data) {
        try {
            byte[] combined = Base64.getDecoder().decode(base64Data);
            if (combined.length < IV_LENGTH + 16) {
                throw new RuntimeException("加密数据长度不足");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            byte[] encrypted = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
}
