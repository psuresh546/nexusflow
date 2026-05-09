package com.spawnbase.credential.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;


@Component
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private final SecretKeySpec secretKey;

    public EncryptionService(
            @Value("${credential.encryption.key}")
            String encryptionKey) {

        // Key must be exactly 16, 24, or 32 bytes for AES
        byte[] keyBytes = encryptionKey.getBytes();
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0,
                Math.min(keyBytes.length, key.length));
        this.secretKey = new SecretKeySpec(key, ALGORITHM);
    }

    /**
     * Encrypt a plain text password.
     * Returns Base64 encoded encrypted string.
     */
    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(
                    plainText.getBytes());
            return Base64.getEncoder()
                    .encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Encryption failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt an encrypted password.
     * Returns the original plain text.
     */
    public String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder()
                    .decode(encryptedText);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException(
                    "Decryption failed: " + e.getMessage(), e);
        }
    }
}