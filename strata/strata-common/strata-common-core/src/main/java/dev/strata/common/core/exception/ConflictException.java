// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.exception;

/**
 * Exception thrown when an operation conflicts with the current state.
 *
 * <p>Common use cases: duplicate resources, version conflicts, state machine violations.
 *
 * <p>Example:
 * <pre>{@code
 * if (userRepository.existsByUsername(username)) {
 *     throw new ConflictException("Username already exists: " + username);
 * }
 * }</pre>
 */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

    public ConflictException(String resourceType, String field, Object value) {
        super(ErrorCode.CONFLICT, resourceType + " with " + field + "='" + value + "' already exists");
    }

    public ConflictException() {
        super(ErrorCode.CONFLICT);
    }
}
