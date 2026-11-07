// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ConfigEncryptionUtil}: encrypt/decrypt roundtrip, ENC(...) wrapping
 * and detection.
 */
class ConfigEncryptionUtilTest {

    private static final String PASSWORD = "test-encryption-key-16ch";

    @Test
    void encryptThenDecryptRoundtrips() {
        String plaintext = "my-secret-password";

        String encrypted = ConfigEncryptionUtil.encrypt(plaintext, PASSWORD);
        String decrypted = ConfigEncryptionUtil.decrypt(encrypted, PASSWORD);

        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void encryptionIsNonDeterministic() {
        assertThat(ConfigEncryptionUtil.encrypt("x", PASSWORD))
                .isNotEqualTo(ConfigEncryptionUtil.encrypt("x", PASSWORD));
    }

    @Test
    void wrapAndUnwrapRoundtrip() {
        String wrapped = ConfigEncryptionUtil.wrap("cipher");

        assertThat(wrapped).isEqualTo("ENC(cipher)");
        assertThat(ConfigEncryptionUtil.unwrap(wrapped)).isEqualTo("cipher");
    }

    @Test
    void encryptAndWrapProducesEnc() {
        String result = ConfigEncryptionUtil.encryptAndWrap("plain", PASSWORD);

        assertThat(ConfigEncryptionUtil.isEncrypted(result)).isTrue();
        assertThat(ConfigEncryptionUtil.decrypt(ConfigEncryptionUtil.unwrap(result), PASSWORD))
                .isEqualTo("plain");
    }

    @Test
    void isEncryptedDetectsWrapper() {
        assertThat(ConfigEncryptionUtil.isEncrypted("ENC(abc)")).isTrue();
        assertThat(ConfigEncryptionUtil.isEncrypted("plain")).isFalse();
        assertThat(ConfigEncryptionUtil.isEncrypted(null)).isFalse();
        assertThat(ConfigEncryptionUtil.isEncrypted("ENC(")).isFalse();
    }

    @Test
    void unwrapPassesThroughUnwrapped() {
        assertThat(ConfigEncryptionUtil.unwrap("plain")).isEqualTo("plain");
    }
}
