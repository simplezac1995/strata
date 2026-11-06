// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.encrypt.aes;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strata.plugin.encrypt.aes")
public class EncryptAesProperties {

    /** Base64-encoded 32-byte AES key. */
    private String key = "";

    private boolean enabled = false;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
