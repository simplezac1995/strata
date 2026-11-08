// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Field-level encryption settings.
 *
 * <p>The key is a Base64-encoded, 32-byte AES-256 key used by {@link FieldEncryptor} for sensitive values such as
 * TOTP secrets. When it is empty the encryptor falls back to an ephemeral random key for local development only:
 * values encrypted in one process cannot be decrypted after a restart or by another instance. Production deployments
 * that use MFA or field encryption must provide a stable key (for example via `STRATA_ENCRYPTION_KEY`).
 */
@Component
@ConfigurationProperties(prefix = "strata.encryption")
public class EncryptionProperties {

    /** Base64-encoded 32-byte AES-256 key; empty means an ephemeral development key. */
    private String key = "";

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
