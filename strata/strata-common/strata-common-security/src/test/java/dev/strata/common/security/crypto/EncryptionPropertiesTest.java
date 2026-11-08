// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EncryptionPropertiesTest {

    @Test
    void defaultsToAnEmptyKey() {
        assertThat(new EncryptionProperties().getKey()).isEmpty();
    }

    @Test
    void storesTheConfiguredKey() {
        EncryptionProperties properties = new EncryptionProperties();
        properties.setKey("base64-key");
        assertThat(properties.getKey()).isEqualTo("base64-key");
    }
}
