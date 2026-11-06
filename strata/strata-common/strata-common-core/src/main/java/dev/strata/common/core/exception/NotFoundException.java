// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.exception;

/**
 * Exception thrown when a requested resource is not found.
 *
 * <p>Use this instead of throwing {@code new BusinessException(ErrorCode.NOT_FOUND)}
 * directly for better type safety and more specific exception handling.
 *
 * <p>Example:
 * <pre>{@code
 * User user = userRepository.findById(id)
 *     .orElseThrow(() -> new NotFoundException("User", id));
 * }</pre>
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String resourceType, Object identifier) {
        super(ErrorCode.NOT_FOUND, resourceType + " not found: " + identifier);
    }

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public NotFoundException() {
        super(ErrorCode.NOT_FOUND);
    }
}
