// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.exception;

import dev.strata.common.core.web.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for Strata Admin.
 *
 * <p>Converts all exceptions to RFC 7807 Problem Details responses with consistent
 * error codes, HTTP status, and human-readable messages. Every exception is logged
 * at an appropriate level for debugging and monitoring.
 *
 * <p>Logging strategy:
 * <ul>
 *   <li>Business exceptions (4xx): WARN level - expected client errors</li>
 *   <li>Validation errors: DEBUG level - detailed field-level information</li>
 *   <li>Server errors (5xx): ERROR level with stack trace - unexpected failures</li>
 *   <li>Unhandled exceptions: ERROR level with stack trace - must be investigated</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // === Business exceptions (4xx) ===

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException exception) {
        ErrorCode errorCode = exception.errorCode();
        log.warn("Business exception: {} - {}", errorCode.name(), exception.getMessage());
        return problem(errorCode, exception.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return problem(ErrorCode.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException exception) {
        log.warn("Conflict: {}", exception.getMessage());
        return problem(ErrorCode.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    public ProblemDetail handleValidationException(ValidationException exception) {
        log.warn("Validation failed: {}", exception.getMessage());
        return problem(ErrorCode.VALIDATION_FAILED, exception.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ProblemDetail handleUnauthorized(UnauthorizedException exception) {
        log.warn("Unauthorized: {}", exception.getMessage());
        return problem(ErrorCode.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException exception) {
        log.warn("Forbidden: {}", exception.getMessage());
        return problem(ErrorCode.FORBIDDEN, exception.getMessage());
    }

    // === Validation exceptions ===

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorDetail> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        log.debug("Validation failed: {} errors", errors.size());
        ProblemDetail detail = problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultMessage());
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldErrorDetail> errors = exception.getConstraintViolations().stream()
                .map(violation ->
                        new FieldErrorDetail(violation.getPropertyPath().toString(), violation.getMessage()))
                .toList();
        log.debug("Constraint violation: {} errors", errors.size());
        ProblemDetail detail = problem(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.defaultMessage());
        detail.setProperty("errors", errors);
        return detail;
    }

    // === Request parsing exceptions ===

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException exception) {
        log.warn("Malformed request body: {}", exception.getMessage());
        return problem(ErrorCode.MALFORMED_REQUEST_BODY, "Malformed request body");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        log.warn("Invalid parameter type: {} - {}", exception.getName(), exception.getMessage());
        return problem(ErrorCode.INVALID_PARAMETER, "Invalid value for parameter '" + exception.getName() + "'");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException exception) {
        log.debug("Resource not found: {}", exception.getMessage());
        return problem(ErrorCode.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException exception) {
        log.warn("Missing parameter: {}", exception.getParameterName());
        return problem(
                ErrorCode.MISSING_PARAMETER, "Missing required parameter '" + exception.getParameterName() + "'");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException exception) {
        log.warn("Method not allowed: {} {}", exception.getMethod(), exception.getSupportedHttpMethods());
        return problem(ErrorCode.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        log.warn("Unsupported media type: {}", exception.getContentType());
        return problem(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail handleMaxUploadSize(MaxUploadSizeExceededException exception) {
        log.warn("Upload size exceeded: {}", exception.getMaxUploadSize());
        return problem(ErrorCode.FILE_SIZE_EXCEEDED, "Upload size exceeds the allowed limit");
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ProblemDetail handleBinding(ServletRequestBindingException exception) {
        log.warn("Request binding error: {}", exception.getMessage());
        return problem(ErrorCode.BAD_REQUEST, "Invalid request: " + exception.getMessage());
    }

    // === Data access exceptions ===

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        log.error("Data integrity violation: {}", exception.getMessage());
        return problem(ErrorCode.DATA_INTEGRITY_VIOLATION, "Data integrity violation");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLocking(OptimisticLockingFailureException exception) {
        log.warn("Optimistic locking failure: {}", exception.getMessage());
        return problem(ErrorCode.RESOURCE_VERSION_CONFLICT, "Resource has been modified by another user");
    }

    @ExceptionHandler(DataAccessException.class)
    public ProblemDetail handleDataAccess(DataAccessException exception) {
        log.error("Database error: {}", exception.getMessage(), exception);
        return problem(ErrorCode.DATABASE_ERROR, "Database error");
    }

    // === Fallback ===

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleOther(Exception exception) {
        String requestInfo = getRequestInfo();
        log.error("Unhandled exception{}: {}", requestInfo, exception.getMessage(), exception);
        return problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage());
    }

    // === Helpers ===

    private ProblemDetail problem(ErrorCode errorCode, String detailText) {
        return ProblemDetails.of(errorCode, detailText);
    }

    private FieldErrorDetail formatFieldError(FieldError error) {
        return new FieldErrorDetail(error.getField(), error.getDefaultMessage());
    }

    private String getRequestInfo() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return " [" + request.getMethod() + " " + request.getRequestURI() + "]";
            }
        } catch (Exception ignored) {
            // Ignore - request context may not be available
        }
        return "";
    }

    /** Structured validation error item: {field, message}. */
    public record FieldErrorDetail(String field, String message) {}
}
