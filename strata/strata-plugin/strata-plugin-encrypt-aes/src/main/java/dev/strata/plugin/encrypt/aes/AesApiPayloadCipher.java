// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.encrypt.aes;

import dev.strata.plugin.api.crypto.ApiPayloadCipher;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AesApiPayloadCipher implements ApiPayloadCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesApiPayloadCipher(byte[] keyBytes) {
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES key must be 32 bytes");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    @Override
    public byte[] encrypt(byte[] plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain);
            byte[] out = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, out, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, out, IV_LENGTH, encrypted.length);
            return out;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES encrypt failed", ex);
        }
    }

    @Override
    public byte[] decrypt(byte[] encryptedPayload) {
        try {
            if (encryptedPayload.length <= IV_LENGTH) {
                throw new IllegalArgumentException("Encrypted payload too short");
            }
            byte[] iv = Arrays.copyOfRange(encryptedPayload, 0, IV_LENGTH);
            byte[] payload = Arrays.copyOfRange(encryptedPayload, IV_LENGTH, encryptedPayload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(payload);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES decrypt failed", ex);
        }
    }
}
