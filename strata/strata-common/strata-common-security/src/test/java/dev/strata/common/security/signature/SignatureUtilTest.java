// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.signature;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SignatureUtilTest {

    private SignatureUtil signatureUtil;
    private static final String SECRET_KEY = "test-secret-key-123456";

    @BeforeEach
    void setUp() {
        signatureUtil = new SignatureUtil();
    }

    @Nested
    @DisplayName("签名生成")
    class SignatureGeneration {

        @Test
        @DisplayName("应生成非空的 Base64 签名")
        void shouldGenerateNonNullBase64Signature() {
            String signature = signatureUtil.generateSignature(
                    "POST", "/api/v1/test", "1694000000000", "nonce-123", "{\"key\":\"value\"}", SECRET_KEY);

            assertThat(signature).isNotNull().isNotEmpty();
            // Base64 字符集验证
            assertThat(signature).matches("^[A-Za-z0-9+/]+={0,2}$");
        }

        @Test
        @DisplayName("相同输入应生成相同签名（确定性）")
        void shouldGenerateSameSignatureForSameInput() {
            String sig1 = signatureUtil.generateSignature(
                    "POST", "/api/v1/test", "1694000000000", "nonce-123", "body", SECRET_KEY);
            String sig2 = signatureUtil.generateSignature(
                    "POST", "/api/v1/test", "1694000000000", "nonce-123", "body", SECRET_KEY);

            assertThat(sig1).isEqualTo(sig2);
        }

        @Test
        @DisplayName("不同方法应生成不同签名")
        void shouldGenerateDifferentSignatureForDifferentMethod() {
            String sigGet = signatureUtil.generateSignature(
                    "GET", "/api/v1/test", "1694000000000", "nonce", "body", SECRET_KEY);
            String sigPost = signatureUtil.generateSignature(
                    "POST", "/api/v1/test", "1694000000000", "nonce", "body", SECRET_KEY);

            assertThat(sigGet).isNotEqualTo(sigPost);
        }

        @Test
        @DisplayName("不同密钥应生成不同签名")
        void shouldGenerateDifferentSignatureForDifferentKey() {
            String sig1 =
                    signatureUtil.generateSignature("POST", "/api/v1/test", "1694000000000", "nonce", "body", "key-1");
            String sig2 =
                    signatureUtil.generateSignature("POST", "/api/v1/test", "1694000000000", "nonce", "body", "key-2");

            assertThat(sig1).isNotEqualTo(sig2);
        }

        @Test
        @DisplayName("GET 请求空 body 应正常生成签名")
        void shouldGenerateSignatureForGetWithEmptyBody() {
            String signature =
                    signatureUtil.generateSignature("GET", "/api/v1/test", "1694000000000", "nonce", "", SECRET_KEY);

            assertThat(signature).isNotNull().isNotEmpty();
        }

        @Test
        @DisplayName("null body 应按空字符串处理")
        void shouldTreatNullBodyAsEmpty() {
            String sigNull =
                    signatureUtil.generateSignature("GET", "/api/v1/test", "1694000000000", "nonce", null, SECRET_KEY);
            String sigEmpty =
                    signatureUtil.generateSignature("GET", "/api/v1/test", "1694000000000", "nonce", "", SECRET_KEY);

            assertThat(sigNull).isEqualTo(sigEmpty);
        }
    }

    @Nested
    @DisplayName("签名验证")
    class SignatureVerification {

        @Test
        @DisplayName("正确签名应验证通过")
        void shouldVerifyValidSignature() {
            String signature = signatureUtil.generateSignature(
                    "POST", "/api/v1/test", "1694000000000", "nonce-123", "{\"key\":\"value\"}", SECRET_KEY);

            boolean valid = signatureUtil.verifySignature(
                    "POST", "/api/v1/test", "1694000000000", "nonce-123", "{\"key\":\"value\"}", SECRET_KEY, signature);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("错误签名应验证失败")
        void shouldRejectInvalidSignature() {
            boolean valid = signatureUtil.verifySignature(
                    "POST", "/api/v1/test", "1694000000000", "nonce", "body", SECRET_KEY, "invalid-signature");

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("篡改 body 应验证失败")
        void shouldRejectTamperedBody() {
            String signature = signatureUtil.generateSignature(
                    "POST", "/api/v1/test", "1694000000000", "nonce", "original-body", SECRET_KEY);

            boolean valid = signatureUtil.verifySignature(
                    "POST", "/api/v1/test", "1694000000000", "nonce", "tampered-body", SECRET_KEY, signature);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("篡改路径应验证失败")
        void shouldRejectTamperedPath() {
            String signature = signatureUtil.generateSignature(
                    "POST", "/api/v1/original", "1694000000000", "nonce", "body", SECRET_KEY);

            boolean valid = signatureUtil.verifySignature(
                    "POST", "/api/v1/tampered", "1694000000000", "nonce", "body", SECRET_KEY, signature);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("null 签名应验证失败")
        void shouldRejectNullSignature() {
            boolean valid = signatureUtil.verifySignature(
                    "POST", "/api/v1/test", "1694000000000", "nonce", "body", SECRET_KEY, null);

            assertThat(valid).isFalse();
        }
    }

    @Nested
    @DisplayName("时间戳验证")
    class TimestampValidation {

        @Test
        @DisplayName("当前时间戳应验证通过")
        void shouldAcceptCurrentTimestamp() {
            String currentTime = String.valueOf(System.currentTimeMillis());

            boolean valid = signatureUtil.isTimestampValid(currentTime, 300);

            assertThat(valid).isTrue();
        }

        @Test
        @DisplayName("过期时间戳应验证失败")
        void shouldRejectExpiredTimestamp() {
            // 10 分钟前
            String oldTime = String.valueOf(System.currentTimeMillis() - 600_000);

            boolean valid = signatureUtil.isTimestampValid(oldTime, 300); // 5 分钟窗口

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("未来时间戳应验证失败")
        void shouldRejectFutureTimestamp() {
            // 10 分钟后
            String futureTime = String.valueOf(System.currentTimeMillis() + 600_000);

            boolean valid = signatureUtil.isTimestampValid(futureTime, 300); // 5 分钟窗口

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("无效时间戳格式应验证失败")
        void shouldRejectInvalidTimestampFormat() {
            boolean valid = signatureUtil.isTimestampValid("not-a-number", 300);

            assertThat(valid).isFalse();
        }

        @Test
        @DisplayName("边界内时间戳应验证通过")
        void shouldAcceptTimestampWithinBoundary() {
            // 刚好 5 分钟前（299 秒前，在 300 秒窗口内）
            String boundaryTime = String.valueOf(System.currentTimeMillis() - 299_000);

            boolean valid = signatureUtil.isTimestampValid(boundaryTime, 300);

            assertThat(valid).isTrue();
        }
    }
}
