// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Unit tests for {@link GlobalExceptionHandler}: every handler maps its exception to the
 * expected RFC 7807 status and error code, without loading a Spring context.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @AfterEach
    void clearContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void handlesBusiness() {
        ProblemDetail pd = handler.handleBusiness(new BusinessException(ErrorCode.NOT_FOUND, "missing"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getTitle()).isEqualTo("NOT_FOUND");
        assertThat(pd.getProperties().get("code")).isEqualTo(40400);
        assertThat(pd.getDetail()).isEqualTo("missing");
    }

    @Test
    void mapsDomainExceptions() {
        assertThat(handler.handleNotFound(new NotFoundException("u", 1)).getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(handler.handleConflict(new ConflictException("c")).getStatus())
                .isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(handler.handleValidationException(new ValidationException("v"))
                        .getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(handler.handleUnauthorized(new UnauthorizedException("u")).getStatus())
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(handler.handleForbidden(new ForbiddenException("f")).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void handlesMethodArgumentNotValid() {
        FieldError fieldError = new FieldError("userRequest", "name", "must not be blank");
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        MethodParameter param = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(param, bindingResult);

        ProblemDetail pd = handler.handleValidation(ex);

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        @SuppressWarnings("unchecked")
        List<GlobalExceptionHandler.FieldErrorDetail> errors = (List<GlobalExceptionHandler.FieldErrorDetail>)
                pd.getProperties().get("errors");
        assertThat(errors).singleElement().satisfies(e -> {
            assertThat(e.field()).isEqualTo("name");
            assertThat(e.message()).isEqualTo("must not be blank");
        });
    }

    @Test
    void handlesConstraintViolation() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(mock(jakarta.validation.Path.class));
        when(violation.getMessage()).thenReturn("too small");
        when(violation.getPropertyPath().toString()).thenReturn("age");

        ProblemDetail pd = handler.handleConstraintViolation(new ConstraintViolationException(Set.of(violation)));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
    }

    @Test
    void handlesRequestParsingExceptions() {
        assertThat(handler.handleUnreadable(new HttpMessageNotReadableException("bad", null))
                        .getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(handler.handleNoResource(
                                new NoResourceFoundException(org.springframework.http.HttpMethod.GET, "/x", null))
                        .getStatus())
                .isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(handler.handleMissingParameter(new MissingServletRequestParameterException("id", "Long"))
                        .getStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(handler.handleMethodNotSupported(new HttpRequestMethodNotSupportedException("PUT"))
                        .getStatus())
                .isEqualTo(HttpStatus.METHOD_NOT_ALLOWED.value());
        assertThat(handler.handleMediaTypeNotSupported(new HttpMediaTypeNotSupportedException("application/xml"))
                        .getStatus())
                .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
        assertThat(handler.handleMaxUploadSize(new MaxUploadSizeExceededException(1024))
                        .getStatus())
                .isEqualTo(HttpStatus.CONTENT_TOO_LARGE.value());
    }

    @Test
    void handlesTypeMismatchAndBinding() {
        MethodParameter param = mock(MethodParameter.class);
        MethodArgumentTypeMismatchException ex =
                new MethodArgumentTypeMismatchException("abc", Long.class, "age", param, null);
        assertThat(handler.handleTypeMismatch(ex).getDetail()).contains("age");

        ServletRequestBindingException binding = mock(ServletRequestBindingException.class);
        when(binding.getMessage()).thenReturn("missing header");
        assertThat(handler.handleBinding(binding).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void handlesDataAccessExceptions() {
        assertThat(handler.handleDataIntegrityViolation(new DataIntegrityViolationException("uk"))
                        .getStatus())
                .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(handler.handleOptimisticLocking(new OptimisticLockingFailureException("stale"))
                        .getStatus())
                .isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(handler.handleDataAccess(new TransientDataAccessException("db down") {})
                        .getStatus())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @Test
    void handlesGenericExceptionWithoutRequestContext() {
        ProblemDetail pd = handler.handleOther(new RuntimeException("boom"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getProperties().get("code")).isEqualTo(50000);
    }

    @Test
    void handlesGenericExceptionWithRequestContext() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        ProblemDetail pd = handler.handleOther(new RuntimeException("boom"));

        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}
