// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.encrypt.aes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AesApiPayloadCipherTest {

    private static final byte[] KEY = new byte[32];

    @Test
    void round_trip() {
        AesApiPayloadCipher cipher = new AesApiPayloadCipher(KEY);
        byte[] plain = "hello-strata".getBytes();
        byte[] encrypted = cipher.encrypt(plain);
        assertThat(cipher.decrypt(encrypted)).isEqualTo(plain);
    }
}
