// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.signature;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for requiring request signature verification on API endpoints.
 *
 * <p>Apply this annotation to controller methods that require request signing to prevent
 * tampering and replay attacks. The client must include the following headers:
 * <ul>
 *   <li>{@code X-Signature}: HMAC-SHA256 signature of the request</li>
 *   <li>{@code X-Timestamp}: Request timestamp (milliseconds since epoch)</li>
 *   <li>{@code X-Nonce}: Unique random string for each request</li>
 * </ul>
 *
 * <p>Signature is computed as: HMAC-SHA256(secret, method + path + timestamp + nonce + body)
 *
 * <p>Usage:
 * <pre>{@code
 * @RequireSignature
 * @PostMapping("/api/v1/payment/create")
 * public Result<PaymentDto> createPayment(@RequestBody PaymentRequest request) { ... }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSignature {

    /**
     * Maximum allowed time skew between request timestamp and server time (in seconds).
     */
    int maxTimeSkewSeconds() default 300;

    /**
     * Whether to validate nonce uniqueness (prevents replay attacks).
     */
    boolean validateNonce() default true;
}
