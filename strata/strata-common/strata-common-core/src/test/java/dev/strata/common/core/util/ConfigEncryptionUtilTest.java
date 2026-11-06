// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ConfigEncryptionUtil}: encrypt/decrypt roundtrip, ENC(...) wrapping
 * and detection.
 */
@DisplayName("ConfigEncryptionUtil 配置加解密")
class ConfigEncryptionUtilTest {

    private static final String PASSWORD = "test-encryption-key-16ch";

    @Test
    @DisplayName("加密后可解密还原明文")
    void encryptThenDecryptRoundtrips() {
        String plaintext = "my-secret-password";

        String encrypted = ConfigEncryptionUtil.encrypt(plaintext, PASSWORD);
        String decrypted = ConfigEncryptionUtil.decrypt(encrypted, PASSWORD);

        assertThat(encrypted).isNotEqualTo(plaintext);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("相同明文两次加密结果不同（随机 IV）")
    void encryptionIsNonDeterministic() {
        assertThat(ConfigEncryptionUtil.encrypt("x", PASSWORD))
                .isNotEqualTo(ConfigEncryptionUtil.encrypt("x", PASSWORD));
    }

    @Test
    @DisplayName("wrap/unwrap 往返")
    void wrapAndUnwrapRoundtrip() {
        String wrapped = ConfigEncryptionUtil.wrap("cipher");

        assertThat(wrapped).isEqualTo("ENC(cipher)");
        assertThat(ConfigEncryptionUtil.unwrap(wrapped)).isEqualTo("cipher");
    }

    @Test
    @DisplayName("encryptAndWrap 产出 ENC(...) 形式")
    void encryptAndWrapProducesEnc() {
        String result = ConfigEncryptionUtil.encryptAndWrap("plain", PASSWORD);

        assertThat(ConfigEncryptionUtil.isEncrypted(result)).isTrue();
        assertThat(ConfigEncryptionUtil.decrypt(ConfigEncryptionUtil.unwrap(result), PASSWORD))
                .isEqualTo("plain");
    }

    @Test
    @DisplayName("isEncrypted 识别 ENC(...) 包裹")
    void isEncryptedDetectsWrapper() {
        assertThat(ConfigEncryptionUtil.isEncrypted("ENC(abc)")).isTrue();
        assertThat(ConfigEncryptionUtil.isEncrypted("plain")).isFalse();
        assertThat(ConfigEncryptionUtil.isEncrypted(null)).isFalse();
        assertThat(ConfigEncryptionUtil.isEncrypted("ENC(")).isFalse();
    }

    @Test
    @DisplayName("unwrap 未包裹的值原样返回")
    void unwrapPassesThroughUnwrapped() {
        assertThat(ConfigEncryptionUtil.unwrap("plain")).isEqualTo("plain");
    }
}
