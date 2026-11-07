// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TotpServiceTest {

    /**
     * RFC 6238 Appendix B test vector key (ASCII "12345678901234567890", 20 bytes) in Base32.
     */
    private static final String RFC_SECRET_BASE32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private final TotpService totpService = new TotpService();

    @Test
    void base32RoundTrip() {
        byte[] original = TotpService.base32Decode(RFC_SECRET_BASE32);
        String encoded = TotpService.base32Encode(original);
        assertThat(encoded).isEqualTo(RFC_SECRET_BASE32);
        assertThat(TotpService.base32Decode(encoded)).isEqualTo(original);
    }

    @Test
    void base32DecodesPaddingAndCase() {
        byte[] a = TotpService.base32Decode("GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ");
        byte[] b = TotpService.base32Decode("gezdgnbvgy3tqojqgezdgnbvgy3tqojq");
        assertThat(b).isEqualTo(a);
    }

    @Test
    void rfc6238Vectors() {
        byte[] key = TotpService.base32Decode(RFC_SECRET_BASE32);
        // T=59  -> counter 1  -> 94287082 -> last 6 = 287082
        assertThat(TotpService.generateCode(key, 1)).isEqualTo("287082");
        // T=1111111109 -> counter 37037036 -> 07081804 -> 081804
        assertThat(TotpService.generateCode(key, 37037036)).isEqualTo("081804");
        // T=1111111111 -> counter 37037037 -> 14050471 -> 050471
        assertThat(TotpService.generateCode(key, 37037037)).isEqualTo("050471");
        // T=1234567890 -> counter 41152263 -> 89005924 -> 005924
        assertThat(TotpService.generateCode(key, 41152263)).isEqualTo("005924");
    }

    @Test
    void verifyAcceptsCurrentCode() {
        String secret = totpService.generateSecret();
        Instant now = Instant.now();
        long counter = now.getEpochSecond() / 30;
        String code = TotpService.generateCode(TotpService.base32Decode(secret), counter);
        assertThat(totpService.verify(secret, code, now)).isTrue();
    }

    @Test
    void verifyAcceptsPreviousStep() {
        String secret = totpService.generateSecret();
        Instant now = Instant.now();
        long counter = now.getEpochSecond() / 30;
        String previousStepCode = TotpService.generateCode(TotpService.base32Decode(secret), counter - 1);
        assertThat(totpService.verify(secret, previousStepCode, now)).isTrue();
    }

    @Test
    void verifyRejectsTwoStepsBack() {
        String secret = totpService.generateSecret();
        Instant now = Instant.now();
        long counter = now.getEpochSecond() / 30;
        String tooOldCode = TotpService.generateCode(TotpService.base32Decode(secret), counter - 2);
        assertThat(totpService.verify(secret, tooOldCode, now)).isFalse();
    }

    @Test
    void verifyRejectsWrongCode() {
        String secret = totpService.generateSecret();
        Instant now = Instant.now();
        long counter = now.getEpochSecond() / 30;
        String correct = TotpService.generateCode(TotpService.base32Decode(secret), counter);
        String wrong = correct.equals("000000") ? "111111" : "000000";
        assertThat(totpService.verify(secret, wrong, now)).isFalse();
    }

    @Test
    void verifyRejectsMalformedCode() {
        String secret = totpService.generateSecret();
        assertThat(totpService.verify(secret, "abc")).isFalse();
        assertThat(totpService.verify(secret, "12345")).isFalse();
        assertThat(totpService.verify(secret, "1234567")).isFalse();
        assertThat(totpService.verify(null, "123456")).isFalse();
        assertThat(totpService.verify(secret, null)).isFalse();
    }

    @Test
    void buildsOtpAuthUri() {
        String uri = totpService.buildOtpAuthUri("Strata Admin", "admin", RFC_SECRET_BASE32);
        assertThat(uri).startsWith("otpauth://totp/");
        assertThat(uri).contains("secret=" + RFC_SECRET_BASE32);
        assertThat(uri).contains("issuer=Strata%20Admin");
        assertThat(uri).contains("period=30").contains("digits=6");
    }

    @Test
    void generatedSecretIsUsable() {
        String secret = totpService.generateSecret();
        assertThat(secret).isNotBlank();
        Instant now = Instant.now();
        long counter = now.getEpochSecond() / 30;
        String code = TotpService.generateCode(TotpService.base32Decode(secret), counter);
        assertThat(totpService.verify(secret, code, now)).isTrue();
    }
}
