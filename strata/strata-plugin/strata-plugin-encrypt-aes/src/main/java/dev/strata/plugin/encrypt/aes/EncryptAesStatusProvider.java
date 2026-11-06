// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.encrypt.aes;

import dev.strata.plugin.api.status.PluginStatusProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Self-declares the {@code encryptAes} status key (ADR-0020).
 *
 * <p>Registered only while the AES encryption module is enabled — mirroring {@link
 * EncryptAesAutoConfiguration} — so the key is omitted from {@code /plugins/status} when the switch
 * is off. Availability additionally requires a configured key: {@link AesApiPayloadCipher} is not
 * usable without one even though the module may be enabled.
 */
@Component
@Order(50)
@ConditionalOnProperty(prefix = "strata.plugin.encrypt.aes", name = "enabled", havingValue = "true")
public class EncryptAesStatusProvider implements PluginStatusProvider {

    private final EncryptAesProperties properties;

    public EncryptAesStatusProvider(EncryptAesProperties properties) {
        this.properties = properties;
    }

    @Override
    public String key() {
        return "encryptAes";
    }

    @Override
    public String label() {
        return "AES request encryption";
    }

    @Override
    public boolean available() {
        return properties.getKey() != null && !properties.getKey().isBlank();
    }

    @Override
    public String detail() {
        return "AES-256 payload cipher and request decrypt filter";
    }
}
