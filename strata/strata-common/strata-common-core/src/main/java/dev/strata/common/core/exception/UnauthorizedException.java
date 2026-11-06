// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.exception;

/**
 * Exception thrown when authentication is required or has failed.
 *
 * <p>Use this for business-level authentication failures, such as expired sessions,
 * invalid tokens, or missing credentials. Framework-level authentication failures
 * are handled by Spring Security.
 *
 * <p>Example:
 * <pre>{@code
 * if (token.isExpired()) {
 *     throw new UnauthorizedException("Token has expired");
 * }
 * }</pre>
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }
}
