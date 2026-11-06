// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.crypto;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Field-level encryption utility for sensitive data at rest.
 *
 * <p>Uses AES-256-GCM authenticated encryption with random IV per encryption.
 * The encryption key is configured via the {@code strata.encryption.key} property
 * (must be a 32-byte Base64-encoded key).
 *
 * <p>Output format: Base64(IV || ciphertext || tag)
 *
 * <p>Usage:
 * <pre>{@code
 * @Autowired
 * private FieldEncryptor encryptor;
 *
 * String encrypted = encryptor.encrypt("sensitive data");
 * String decrypted = encryptor.decrypt(encrypted);
 * }</pre>
 */
@Component
public class FieldEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public FieldEncryptor(@Value("${strata.encryption.key:}") String base64Key) {
        if (base64Key == null || base64Key.isEmpty()) {
            // Generate a random key for development (not suitable for production!)
            byte[] keyBytes = new byte[32];
            secureRandom.nextBytes(keyBytes);
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(base64Key);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("Encryption key must be 32 bytes (256 bits)");
            }
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        }
    }

    /**
     * Encrypt a plaintext string.
     *
     * @param plaintext the string to encrypt
     * @return Base64-encoded encrypted value
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypt an encrypted string.
     *
     * @param encrypted Base64-encoded encrypted value
     * @return decrypted plaintext
     */
    public String decrypt(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(encrypted);

            // Extract IV and ciphertext
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * Check if a value appears to be encrypted (Base64 with correct length pattern).
     * This is a heuristic check, not definitive.
     */
    public boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length > IV_LENGTH;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
