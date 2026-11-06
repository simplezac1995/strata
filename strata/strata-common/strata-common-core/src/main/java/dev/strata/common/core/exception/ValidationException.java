// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.exception;

/**
 * Exception thrown when request validation fails.
 *
 * <p>Use this for business rule validation that goes beyond Bean Validation annotations.
 * For standard field validation, use {@code @Valid} and {@code ConstraintViolationException}.
 *
 * <p>Example:
 * <pre>{@code
 * if (request.endDate().isBefore(request.startDate())) {
 *     throw new ValidationException("End date must be after start date");
 * }
 * }</pre>
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
    }

    public ValidationException(String field, String message) {
        super(ErrorCode.VALIDATION_FAILED, "Validation failed for '" + field + "': " + message);
    }

    public ValidationException() {
        super(ErrorCode.VALIDATION_FAILED);
    }
}
