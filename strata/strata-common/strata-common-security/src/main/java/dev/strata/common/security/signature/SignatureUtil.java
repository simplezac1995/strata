// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.signature;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Utility class for generating and verifying HMAC request signatures.
 *
 * <p>Signature algorithm: HMAC-SHA256
 * <p>Signature string format: {@code method + path + timestamp + nonce + body}
 *
 * <p>Example:
 * <pre>{@code
 * String signature = SignatureUtil.generateSignature(
 *     "POST",
 *     "/api/v1/payment/create",
 *     "1694000000000",
 *     "random-nonce-123",
 *     "{\"amount\":100}",
 *     "your-secret-key"
 * );
 * }</pre>
 */
@Component
public class SignatureUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Generate HMAC-SHA256 signature for a request.
     *
     * @param method HTTP method (GET, POST, PUT, DELETE)
     * @param path Request path
     * @param timestamp Request timestamp (milliseconds since epoch)
     * @param nonce Unique random string
     * @param body Request body (can be empty for GET requests)
     * @param secretKey Secret key for HMAC
     * @return Base64-encoded signature
     */
    public String generateSignature(
            String method, String path, String timestamp, String nonce, String body, String secretKey) {
        String signatureString = buildSignatureString(method, path, timestamp, nonce, body);
        return hmacSha256(signatureString, secretKey);
    }

    /**
     * Verify a request signature.
     *
     * @param method HTTP method
     * @param path Request path
     * @param timestamp Request timestamp
     * @param nonce Unique random string
     * @param body Request body
     * @param secretKey Secret key for HMAC
     * @param providedSignature Signature provided by the client
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(
            String method,
            String path,
            String timestamp,
            String nonce,
            String body,
            String secretKey,
            String providedSignature) {
        String expectedSignature = generateSignature(method, path, timestamp, nonce, body, secretKey);
        return constantTimeEquals(expectedSignature, providedSignature);
    }

    /**
     * Check if a timestamp is within the allowed time skew.
     *
     * @param timestamp Request timestamp (milliseconds since epoch)
     * @param maxSkewSeconds Maximum allowed time skew in seconds
     * @return true if timestamp is valid, false otherwise
     */
    public boolean isTimestampValid(String timestamp, int maxSkewSeconds) {
        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = System.currentTimeMillis();
            long skew = Math.abs(currentTime - requestTime);
            return skew <= (long) maxSkewSeconds * 1000;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Build the signature string from request components.
     */
    private String buildSignatureString(String method, String path, String timestamp, String nonce, String body) {
        StringBuilder sb = new StringBuilder();
        sb.append(method.toUpperCase()).append("\n");
        sb.append(path).append("\n");
        sb.append(timestamp).append("\n");
        sb.append(nonce).append("\n");
        sb.append(body != null ? body : "");
        return sb.toString();
    }

    /**
     * Compute HMAC-SHA256 and return Base64-encoded result.
     */
    private String hmacSha256(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }
}
