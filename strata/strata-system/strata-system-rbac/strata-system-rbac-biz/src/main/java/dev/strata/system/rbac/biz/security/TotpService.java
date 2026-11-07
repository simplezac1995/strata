// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Self-contained RFC 6238 TOTP (HMAC-SHA1, 30-second time step, 6 digits) plus RFC 4648 Base32 secret
 * handling. No external TOTP library is pulled in: {@code javax.crypto.Mac} is sufficient and avoids a
 * heavy dependency.
 *
 * <p>This class is pure and side-effect free so it can be unit-tested against RFC 6238 test vectors and the
 * replay window boundary without Spring or a database.
 */
@Component
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int DIGITS = 6;
    private static final long STEP_SECONDS = 30L;
    /** Accept the current step and one step either side: tolerates clock skew up to 30 s. */
    private static final int WINDOW = 1;

    private static final int MODULO = (int) Math.pow(10, DIGITS);

    /**
     * Generates a new 160-bit (20-byte) Base32 secret, RFC 4648 without padding — the form authenticator
     * apps expect.
     */
    public String generateSecret() {
        byte[] key = new byte[20];
        SECURE_RANDOM.nextBytes(key);
        return base32Encode(key);
    }

    /**
     * Builds the {@code otpauth://totp/...} URI an authenticator app scans. The label is
     * {@code issuer:account} and {@code issuer} is repeated as a query param, per the Google Authenticator
     * key URI spec.
     */
    public String buildOtpAuthUri(String issuer, String account, String base32Secret) {
        String label = issuer + ":" + account;
        return "otpauth://totp/"
                + urlEncode(label)
                + "?secret=" + base32Secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS
                + "&period=" + STEP_SECONDS;
    }

    /**
     * Verifies a submitted 6-digit code against the secret at the current time, allowing one time step of
     * clock skew on each side (the RFC 6238 recommended replay window).
     *
     * @param base32Secret the user's secret (Base32, padded or unpadded)
     * @param code         the code the admin submitted (digits only)
     * @return true when the code matches within the replay window
     */
    public boolean verify(String base32Secret, String code) {
        return verify(base32Secret, code, Instant.now());
    }

    /** Testable variant pinned to a specific instant. */
    boolean verify(String base32Secret, String code, Instant now) {
        if (base32Secret == null || code == null) {
            return false;
        }
        String normalized = code.replaceAll("\\s", "");
        if (!normalized.matches("\\d{" + DIGITS + "}")) {
            return false;
        }
        byte[] key;
        try {
            key = base32Decode(base32Secret);
        } catch (IllegalArgumentException invalid) {
            return false;
        }
        long counter = now.getEpochSecond() / STEP_SECONDS;
        for (long step = counter - WINDOW; step <= counter + WINDOW; step++) {
            if (step >= 0 && constantTimeEquals(generateCode(key, step), normalized)) {
                return true;
            }
        }
        return false;
    }

    /** RFC 6238 HOTP derivation for a counter. Visible for testing against known vectors. */
    static String generateCode(byte[] key, long counter) {
        byte[] msg = ByteBuffer.allocate(8).putLong(counter).array();
        byte[] hash = hmacSha1(key, msg);
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) << 8)
                | (hash[offset + 3] & 0xFF);
        int otp = binary % MODULO;
        return String.format(Locale.ROOT, "%0" + DIGITS + "d", otp);
    }

    private static byte[] hmacSha1(byte[] key, byte[] msg) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
            mac.init(new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1"));
            return mac.doFinal(msg);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA1 unavailable", e);
        }
    }

    /** Constant-time comparison so verification timing does not leak the matching prefix. */
    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    static String base32Encode(byte[] data) {
        StringBuilder out = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                out.append(BASE32_ALPHABET.charAt(index));
            }
        }
        if (bitsLeft > 0) {
            out.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 0x1F));
        }
        return out.toString();
    }

    static byte[] base32Decode(String encoded) {
        String cleaned = encoded.trim().replace("=", "").toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        byte[] out = new byte[cleaned.length() * 5 / 8];
        int pos = 0;
        for (char c : cleaned.toCharArray()) {
            int index = BASE32_ALPHABET.indexOf(c);
            if (index < 0) {
                throw new IllegalArgumentException("Illegal Base32 character: " + c);
            }
            buffer = (buffer << 5) | index;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                out[pos++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        if (pos == 0) {
            throw new IllegalArgumentException("Empty Base32 input");
        }
        return pos == out.length ? out : java.util.Arrays.copyOf(out, pos);
    }

    private static String urlEncode(String value) {
        return value.replace(":", "%3A").replace("@", "%40").replace(" ", "%20");
    }

    private static final java.security.SecureRandom SECURE_RANDOM = new java.security.SecureRandom();
}
