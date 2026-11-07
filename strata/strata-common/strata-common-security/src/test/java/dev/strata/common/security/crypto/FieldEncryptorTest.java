// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.crypto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
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
    class EncryptionTests {

        @Test
        void shouldEncryptPlainString() {
            String plaintext = "Hello, World!";
            String encrypted = encryptor.encrypt(plaintext);

            assertThat(encrypted).isNotNull().isNotEmpty();
            assertThat(encrypted).isNotEqualTo(plaintext);
            // 验证是 Base64 编码
            assertThat(encrypted).matches("^[A-Za-z0-9+/]+={0,2}$");
        }

        @Test
        void shouldGenerateDifferentCiphertextForSamePlaintext() {
            String plaintext = "sensitive data";

            String encrypted1 = encryptor.encrypt(plaintext);
            String encrypted2 = encryptor.encrypt(plaintext);

            assertThat(encrypted1).isNotEqualTo(encrypted2);
        }

        @Test
        void shouldReturnNullForNullPlaintext() {
            assertThat(encryptor.encrypt(null)).isNull();
        }

        @Test
        void shouldEncryptEmptyString() {
            String encrypted = encryptor.encrypt("");

            assertThat(encrypted).isNotNull().isNotEmpty();
        }

        @Test
        void shouldEncryptUnicodeAndSpecialChars() {
            String plaintext = "你好，世界！🌍 特殊字符: @#$%^&*()";

            String encrypted = encryptor.encrypt(plaintext);
            String decrypted = encryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
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
    class DecryptionTests {

        @Test
        void shouldDecryptCiphertext() {
            String plaintext = "Hello, World!";
            String encrypted = encryptor.encrypt(plaintext);

            String decrypted = encryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo(plaintext);
        }

        @Test
        void shouldReturnNullForNullCiphertext() {
            assertThat(encryptor.decrypt(null)).isNull();
        }

        @Test
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
    class EncryptionDetectionTests {

        @Test
        void shouldDetectEncryptedValue() {
            String encrypted = encryptor.encrypt("test data");

            assertThat(encryptor.isEncrypted(encrypted)).isTrue();
        }

        @Test
        void shouldDetectPlainTextAsNotEncrypted() {
            assertThat(encryptor.isEncrypted("just plain text")).isFalse();
        }

        @Test
        void shouldReturnFalseForNullOrEmpty() {
            assertThat(encryptor.isEncrypted(null)).isFalse();
            assertThat(encryptor.isEncrypted("")).isFalse();
        }

        @Test
        void shouldReturnFalseForInvalidBase64() {
            assertThat(encryptor.isEncrypted("not valid base64!!!")).isFalse();
        }
    }

    @Nested
    class KeyValidationTests {

        @Test
        void shouldGenerateRandomKeyForEmptyKey() {
            FieldEncryptor devEncryptor = new FieldEncryptor("");

            // 应该能正常加密解密
            String encrypted = devEncryptor.encrypt("test");
            String decrypted = devEncryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo("test");
        }

        @Test
        void shouldGenerateRandomKeyForNullKey() {
            FieldEncryptor devEncryptor = new FieldEncryptor(null);

            String encrypted = devEncryptor.encrypt("test");
            String decrypted = devEncryptor.decrypt(encrypted);

            assertThat(decrypted).isEqualTo("test");
        }

        @Test
        void shouldThrowForInvalidKeyLength() {
            String shortKey = Base64.getEncoder().encodeToString("short".getBytes());

            assertThatThrownBy(() -> new FieldEncryptor(shortKey))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("32 bytes");
        }
    }

    @Nested
    class RoundTripTests {

        @Test
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
