// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Unified error code enumeration for Strata Admin.
 *
 * <p>Error code ranges:
 * <ul>
 *   <li>40000-40099: Client errors (bad request, validation)</li>
 *   <li>40100-40199: Authentication errors</li>
 *   <li>40300-40399: Authorization errors</li>
 *   <li>40400-40499: Not found errors</li>
 *   <li>40900-40999: Conflict errors</li>
 *   <li>42200-42299: Unprocessable entity (business rule violations)</li>
 *   <li>42900-42999: Rate limiting</li>
 *   <li>50000-50099: Server errors</li>
 *   <li>50200-50299: Upstream/gateway errors</li>
 * </ul>
 */
public enum ErrorCode {
    // === 400xx: Client errors ===
    BAD_REQUEST(40000, HttpStatus.BAD_REQUEST, "Bad request"),
    VALIDATION_FAILED(40001, HttpStatus.UNPROCESSABLE_CONTENT, "Validation failed"),
    IDEMPOTENCY_KEY_REQUIRED(40002, HttpStatus.BAD_REQUEST, "Idempotency-Key required"),
    INVALID_PARAMETER(40003, HttpStatus.BAD_REQUEST, "Invalid parameter"),
    MISSING_PARAMETER(40004, HttpStatus.BAD_REQUEST, "Missing required parameter"),
    MALFORMED_REQUEST_BODY(40005, HttpStatus.BAD_REQUEST, "Malformed request body"),
    UNSUPPORTED_OPERATION(40006, HttpStatus.BAD_REQUEST, "Unsupported operation"),
    CAPTCHA_REQUIRED(40010, HttpStatus.BAD_REQUEST, "Captcha verification is required"),
    CAPTCHA_INVALID(40011, HttpStatus.BAD_REQUEST, "Captcha answer is incorrect"),
    CAPTCHA_EXPIRED(40012, HttpStatus.BAD_REQUEST, "Captcha has expired or was already used"),

    // === 401xx: Authentication errors ===
    UNAUTHORIZED(40100, HttpStatus.UNAUTHORIZED, "Unauthorized"),
    INVALID_CREDENTIALS(40101, HttpStatus.UNAUTHORIZED, "Invalid credentials"),
    TOKEN_EXPIRED(40102, HttpStatus.UNAUTHORIZED, "Token has expired"),
    TOKEN_INVALID(40103, HttpStatus.UNAUTHORIZED, "Invalid token"),
    TOKEN_REVOKED(40104, HttpStatus.UNAUTHORIZED, "Token has been revoked"),
    SESSION_EXPIRED(40105, HttpStatus.UNAUTHORIZED, "Session has expired"),
    ACCOUNT_LOCKED(40106, HttpStatus.UNAUTHORIZED, "Account is locked"),
    ACCOUNT_DISABLED(40107, HttpStatus.UNAUTHORIZED, "Account is disabled"),
    CRYPTO_KEY_UNKNOWN(40108, HttpStatus.UNAUTHORIZED, "Signing key not found or expired"),
    CRYPTO_TIMESTAMP_OUT_OF_WINDOW(40109, HttpStatus.UNAUTHORIZED, "Request timestamp outside the allowed window"),
    CRYPTO_SIGNATURE_INVALID(40110, HttpStatus.UNAUTHORIZED, "Request signature is invalid"),
    INVALID_SIGNATURE(40111, HttpStatus.UNAUTHORIZED, "Payload signature verification failed"),
    MFA_REQUIRED(40112, HttpStatus.UNAUTHORIZED, "Second factor authentication required"),
    MFA_INVALID_CODE(40113, HttpStatus.UNAUTHORIZED, "Invalid verification code"),
    MFA_CHALLENGE_EXPIRED(40114, HttpStatus.UNAUTHORIZED, "MFA challenge has expired or was already used"),
    MFA_NOT_ENROLLED(40115, HttpStatus.UNAUTHORIZED, "MFA has not been enrolled"),

    // === 403xx: Authorization errors ===
    FORBIDDEN(40300, HttpStatus.FORBIDDEN, "Forbidden"),
    PLAYER_LOCKED(40301, HttpStatus.FORBIDDEN, "Account temporarily locked"),
    PLAYER_NOT_ACTIVE(40302, HttpStatus.FORBIDDEN, "Account is not active"),
    INSUFFICIENT_PERMISSIONS(40303, HttpStatus.FORBIDDEN, "Insufficient permissions"),
    TENANT_ACCESS_DENIED(40304, HttpStatus.FORBIDDEN, "Tenant access denied"),
    RESOURCE_ACCESS_DENIED(40305, HttpStatus.FORBIDDEN, "Resource access denied"),
    FEATURE_NOT_ENABLED(40306, HttpStatus.FORBIDDEN, "Feature is not enabled"),
    CSRF_TOKEN_INVALID(40307, HttpStatus.FORBIDDEN, "CSRF token invalid"),
    LAUNCH_TOKEN_FOREIGN(40308, HttpStatus.FORBIDDEN, "Launch token does not belong to this session"),

    // === 404xx: Not found errors ===
    NOT_FOUND(40400, HttpStatus.NOT_FOUND, "Resource not found"),
    USER_NOT_FOUND(40401, HttpStatus.NOT_FOUND, "User not found"),
    ROLE_NOT_FOUND(40402, HttpStatus.NOT_FOUND, "Role not found"),
    MENU_NOT_FOUND(40403, HttpStatus.NOT_FOUND, "Menu not found"),
    DEPARTMENT_NOT_FOUND(40404, HttpStatus.NOT_FOUND, "Department not found"),
    TENANT_NOT_FOUND(40405, HttpStatus.NOT_FOUND, "Tenant not found"),
    CONFIG_NOT_FOUND(40406, HttpStatus.NOT_FOUND, "Configuration not found"),
    FILE_NOT_FOUND(40407, HttpStatus.NOT_FOUND, "File not found"),

    // === 405xx: Method not allowed ===
    METHOD_NOT_ALLOWED(40500, HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed"),

    // === 409xx: Conflict errors ===
    CONFLICT(40900, HttpStatus.CONFLICT, "Conflict"),
    IDEMPOTENCY_IN_PROGRESS(40901, HttpStatus.CONFLICT, "Identical request in progress"),
    IDEMPOTENCY_KEY_REUSED(40902, HttpStatus.CONFLICT, "Idempotency-Key reused for another request"),
    DUPLICATE_LEDGER_ENTRY(40903, HttpStatus.CONFLICT, "Ledger entry already posted"),
    EMAIL_ALREADY_REGISTERED(40904, HttpStatus.CONFLICT, "Email already registered"),
    USERNAME_ALREADY_EXISTS(40905, HttpStatus.CONFLICT, "Username already exists"),
    ROLE_CODE_ALREADY_EXISTS(40906, HttpStatus.CONFLICT, "Role code already exists"),
    RESOURCE_VERSION_CONFLICT(40907, HttpStatus.CONFLICT, "Resource version conflict"),
    CRYPTO_NONCE_REPLAYED(40908, HttpStatus.CONFLICT, "Request nonce has been replayed"),
    BONUS_ALREADY_CLAIMED(40909, HttpStatus.CONFLICT, "Bonus already claimed"),
    BONUS_ACTIVE_EXISTS(40910, HttpStatus.CONFLICT, "An active bonus is already in progress"),
    VIP_BONUS_ALREADY_CLAIMED(40911, HttpStatus.CONFLICT, "VIP monthly bonus already claimed this period"),

    // === 410xx: Gone (one-time tokens) ===
    LAUNCH_TOKEN_EXPIRED(41000, HttpStatus.GONE, "Launch token is expired or already used"),

    // === 413xx: Payload too large ===
    PAYLOAD_TOO_LARGE(41300, HttpStatus.CONTENT_TOO_LARGE, "Payload too large"),
    FILE_SIZE_EXCEEDED(41301, HttpStatus.CONTENT_TOO_LARGE, "File size exceeds the allowed limit"),

    // === 415xx: Unsupported media type ===
    UNSUPPORTED_MEDIA_TYPE(41500, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),

    // === 422xx: Unprocessable entity ===
    INSUFFICIENT_FUNDS(42200, HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient funds"),
    ASSET_NOT_SUPPORTED(42201, HttpStatus.UNPROCESSABLE_CONTENT, "Asset or network not supported"),
    UNKNOWN_REFERRAL_CODE(42202, HttpStatus.UNPROCESSABLE_CONTENT, "Unknown referral code"),
    BUSINESS_RULE_VIOLATION(42203, HttpStatus.UNPROCESSABLE_CONTENT, "Business rule violation"),
    INVALID_STATE_TRANSITION(42204, HttpStatus.UNPROCESSABLE_CONTENT, "Invalid state transition"),
    OPERATION_NOT_ALLOWED(42205, HttpStatus.UNPROCESSABLE_CONTENT, "Operation not allowed in current state"),
    DATA_INTEGRITY_VIOLATION(42206, HttpStatus.UNPROCESSABLE_CONTENT, "Data integrity violation"),
    // The player-facing wallet contract names this WALLET_INSUFFICIENT_BALANCE; the client keys on
    // this exact string to show a top-up prompt, which is why it is its own code rather than INSUFFICIENT_FUNDS.
    WALLET_INSUFFICIENT_BALANCE(42207, HttpStatus.UNPROCESSABLE_CONTENT, "Insufficient wallet balance"),

    // === 429xx: Rate limiting ===
    TOO_MANY_REQUESTS(42900, HttpStatus.TOO_MANY_REQUESTS, "Too many requests"),
    RATE_LIMIT_EXCEEDED(42901, HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"),
    HANDSHAKE_RATE_LIMITED(42902, HttpStatus.TOO_MANY_REQUESTS, "Handshake rate limit exceeded"),

    // === 500xx: Server errors ===
    INTERNAL_ERROR(50000, HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"),
    DATABASE_ERROR(50001, HttpStatus.INTERNAL_SERVER_ERROR, "Database error"),
    CACHE_ERROR(50002, HttpStatus.INTERNAL_SERVER_ERROR, "Cache error"),
    MESSAGE_QUEUE_ERROR(50003, HttpStatus.INTERNAL_SERVER_ERROR, "Message queue error"),
    FILE_STORAGE_ERROR(50004, HttpStatus.INTERNAL_SERVER_ERROR, "File storage error"),
    EXTERNAL_SERVICE_ERROR(50005, HttpStatus.INTERNAL_SERVER_ERROR, "External service error"),
    CONFIGURATION_ERROR(50006, HttpStatus.INTERNAL_SERVER_ERROR, "Configuration error"),

    // === 502xx: Upstream/gateway errors ===
    UPSTREAM_UNAVAILABLE(50200, HttpStatus.BAD_GATEWAY, "Upstream provider unavailable"),
    UPSTREAM_REJECTED(50201, HttpStatus.BAD_GATEWAY, "Upstream provider rejected the request"),
    UPSTREAM_TIMEOUT(50202, HttpStatus.BAD_GATEWAY, "Upstream provider timeout"),
    UPSTREAM_RESPONSE_INVALID(50203, HttpStatus.BAD_GATEWAY, "Upstream provider returned invalid response"),

    // === 503xx: Service unavailable ===
    SERVICE_UNAVAILABLE(50300, HttpStatus.SERVICE_UNAVAILABLE, "Service unavailable"),
    MAINTENANCE_MODE(50301, HttpStatus.SERVICE_UNAVAILABLE, "Service is under maintenance"),
    CIRCUIT_BREAKER_OPEN(50302, HttpStatus.SERVICE_UNAVAILABLE, "Circuit breaker is open");

    private final int code;
    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(int code, HttpStatus status, String defaultMessage) {
        this.code = code;
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public int code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
