// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FieldEncryptorTest {

    private FieldEncryptor encryptor;
    private static final String TEST_KEY =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    @BeforeEach
    void setUp() {
        encryptor = new FieldEncryptor(TEST_KEY);
    }

    @Nested
    @DisplayName("加密功能")
    class EncryptionTests {

        @Test
        @DisplayName("应加密普通字符串")
        void shouldEncryptPlainString() {
            String plaintext = "Hello, World!";
            String encrypted = encryptor.encrypt(plaintext);

            assertThat(encrypted).isNotNull().isNotEmpty();
            assertThat(encrypted).isNotEqualTo(plaintext);
            // 验证是 Base64 编码
            assertThat(encrypted).matches("^[A-Za-z0-9+/]+={0,2}$");
        }

        @Test
        @DisplayName("相同明文每次加密应生成不同密文（随机 IV）")
        void shouldGenerateDifferentCiphertextForSamePlaintext() {
            String plaintext = "sensitive data";

            String encrypted1 = encryptor.encrypt(plaintext);
            String encrypted2 = encryptor.encrypt(plaintext);

            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        @DisplayName("null 明文应返回 null")
        void shouldReturnNullForNullPlaintext() {
            assertThat(encryptor.encrypt(null)).isNull();
        }

        @Test
        @DisplayName("空字符串应正常加密")
        void shouldEncryptEmptyString() {
            String encrypted = encryptor.encrypt("");

            assertThat(encrypted).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("中文和特殊字符应正常加密")
        void shouldEncryptUnicodeAndSpecialChars() {
            String plaintext = "你好，世界！🌍 特殊字符: @#$%^&*()";

            String encrypted = encryptor.encrypt(plaintext);
            String decrypted = encryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("长字符串应正常加密")
        void shouldEncryptLongString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("A");
            }
            String plaintext = sb.toString();

            String encrypted = encryptor.encrypt(plaintext);
            String decrypted = encryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(plaintext);
        }
    }

    @Nested
    @DisplayName("解密功能")
    class DecryptionTests {

        @Test
        @DisplayName("应正确解密密文")
        void shouldDecryptCiphertext() {
            String plaintext = "Hello, World!";
            String encrypted = encryptor.encrypt(plaintext);

            String decrypted = encryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        @DisplayName("null 密文应返回 null")
        void shouldReturnNullForNullCiphertext() {
            assertThat(encryptor.decrypt(null)).isNull();
        }

        @Test
        @DisplayName("篡改密文应解密失败")
        void shouldFailToDecryptTamperedCiphertext() {
            String encrypted = encryptor.encrypt("secret data");
            // 篡改最后一个字符
            char[] chars = encrypted.toCharArray();
            chars[chars.length - 1] = (chars[chars.length - 1] == 'A') ? 'B' : 'A';
            String tampered = new String(chars);

            assertThatThrownBy(() -> encryptor.decrypt(tampered))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Decryption failed");
        }

        @Test
        @DisplayName("使用错误密钥解密应失败")
        void shouldFailToDecryptWithWrongKey() {
            String encrypted = encryptor.encrypt("secret data");

            String wrongKey = Base64.getEncoder().encodeToString("fedcba9876543210fedcba9876543210".getBytes());
            FieldEncryptor wrongEncryptor = new FieldEncryptor(wrongKey);

            assertThatThrownBy(() -> wrongEncryptor.decrypt(encrypted))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Decryption failed");
        }
    }

    @Nested
    @DisplayName("加密检测")
    class EncryptionDetectionTests {

        @Test
        @DisplayName("应检测到已加密的值")
        void shouldDetectEncryptedValue() {
            String encrypted = encryptor.encrypt("test data");

            assertThat(encryptor.isEncrypted(encrypted)).isTrue();
        }

        @Test
        @DisplayName("应检测到普通文本未加密")
        void shouldDetectPlainTextAsNotEncrypted() {
            assertThat(encryptor.isEncrypted("just plain text")).isFalse();
        }

        @Test
        @DisplayName("null 和空字符串应返回 false")
        void shouldReturnFalseForNullOrEmpty() {
            assertThat(encryptor.isEncrypted(null)).isFalse();
            assertThat(encryptor.isEncrypted("")).isFalse();
        }

        @Test
        @DisplayName("无效 Base64 应返回 false")
        void shouldReturnFalseForInvalidBase64() {
            assertThat(encryptor.isEncrypted("not valid base64!!!")).isFalse();
        }
    }

    @Nested
    @DisplayName("密钥验证")
    class KeyValidationTests {

        @Test
        @DisplayName("空密钥应生成随机开发密钥")
        void shouldGenerateRandomKeyForEmptyKey() {
            FieldEncryptor devEncryptor = new FieldEncryptor("");

            // 应该能正常加密解密
            String encrypted = devEncryptor.encrypt("test");
            String decrypted = devEncryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo("test");
        }

        @Test
        @DisplayName("null 密钥应生成随机开发密钥")
        void shouldGenerateRandomKeyForNullKey() {
            FieldEncryptor devEncryptor = new FieldEncryptor(null);

            String encrypted = devEncryptor.encrypt("test");
            String decrypted = devEncryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo("test");
        }

        @Test
        @DisplayName("长度不是 32 字节的密钥应抛出异常")
        void shouldThrowForInvalidKeyLength() {
            String shortKey = Base64.getEncoder().encodeToString("short".getBytes());

            assertThatThrownBy(() -> new FieldEncryptor(shortKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("32 bytes");
        }
    }

    @Nested
    @DisplayName("往返测试")
    class RoundTripTests {

        @Test
        @DisplayName("加密-解密往返应保持数据完整性")
        void shouldPreserveDataIntegrityThroughRoundTrip() {
            String[] testCases = {
                "simple",
                "with spaces",
                "with-special-chars-!@#$%^&*()",
                "数字1234567890",
                "日本語テスト",
                "한국어 테스트",
                "emoji 🌍🚀💻",
                "multi\nline\ntext",
                "\t\ttabbed\t\t",
            };

            for (String original : testCases) {
                String encrypted = encryptor.encrypt(original);
                String decrypted = encryptor.decrypt(encrypted);
                assertThat(decrypted).as("Round trip failed for: %s", original).isEqualTo(original);
            }
        }
    }
}
