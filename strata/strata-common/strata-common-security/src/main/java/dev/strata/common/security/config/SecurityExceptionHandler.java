// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.config;

import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns method-security rejections into Problem Details. Each rejection is also logged: a 403 that
 * only exists as an HTTP response cannot be correlated with the user's report, and permission
 * denials are exactly what an operator needs to see (docs/05 §日志与排错).
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class SecurityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SecurityExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        log.warn("{} {} denied: {}", request.getMethod(), request.getRequestURI(), ErrorCode.FORBIDDEN.name());
        return ProblemDetails.of(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException exception, HttpServletRequest request) {
        log.warn("{} {} rejected: {}", request.getMethod(), request.getRequestURI(), ErrorCode.UNAUTHORIZED.name());
        return ProblemDetails.of(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.defaultMessage());
    }
}
