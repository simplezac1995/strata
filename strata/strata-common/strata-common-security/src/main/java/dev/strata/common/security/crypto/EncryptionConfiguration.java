// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for field-level encryption.
 *
 * <p>Initializes the {@link FieldEncryptor} and wires it into the
 * {@link EncryptedFieldListener} for JPA entity field encryption.
 *
 * <p>Configure the encryption key via {@code strata.encryption.key} property
 * (32-byte Base64-encoded AES-256 key). When unset, {@link FieldEncryptor} falls back
 * to an ephemeral random key for local development — encrypted secrets do NOT survive a
 * restart without it, so production must inject the key via environment variable.
 */
@Configuration
public class EncryptionConfiguration {

    @Bean
    public FieldEncryptor fieldEncryptor(@Value("${strata.encryption.key:}") String base64Key) {
        return new FieldEncryptor(base64Key);
    }

    @Bean
    public EncryptedFieldListener encryptedFieldListener(FieldEncryptor fieldEncryptor) {
        EncryptedFieldListener listener = new EncryptedFieldListener();
        EncryptedFieldListener.setFieldEncryptor(fieldEncryptor);
        return listener;
    }
}
