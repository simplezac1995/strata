// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for the exception hierarchy and {@link ErrorCode} metadata.
 */
@DisplayName("业务异常层级与错误码")
class BusinessExceptionHierarchyTest {

    @Test
    @DisplayName("ErrorCode 携带 code/status/defaultMessage")
    void errorCodeMetadata() {
        ErrorCode code = ErrorCode.NOT_FOUND;

        assertThat(code.code()).isEqualTo(40400);
        assertThat(code.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(code.defaultMessage()).isEqualTo("Resource not found");
    }

    @Test
    @DisplayName("BusinessException 默认消息取错误码 defaultMessage")
    void businessExceptionUsesDefaultMessage() {
        BusinessException ex = new BusinessException(ErrorCode.BAD_REQUEST);

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("Bad request");
    }

    @Test
    @DisplayName("BusinessException 自定义消息覆盖默认")
    void businessExceptionCustomMessage() {
        BusinessException ex = new BusinessException(ErrorCode.BAD_REQUEST, "boom");

        assertThat(ex.getMessage()).isEqualTo("boom");
    }

    @Test
    @DisplayName("NotFoundException 消息拼接资源类型与标识")
    void notFoundExceptionMessage() {
        NotFoundException byId = new NotFoundException("User", 42L);
        assertThat(byId.getMessage()).isEqualTo("User not found: 42");
        assertThat(byId.errorCode()).isEqualTo(ErrorCode.NOT_FOUND);

        NotFoundException plain = new NotFoundException("missing");
        assertThat(plain.getMessage()).isEqualTo("missing");

        NotFoundException defaultEx = new NotFoundException();
        assertThat(defaultEx.getMessage()).isEqualTo(ErrorCode.NOT_FOUND.defaultMessage());
    }

    @Test
    @DisplayName("ConflictException 三种构造器")
    void conflictExceptionConstructors() {
        assertThat(new ConflictException("dup").errorCode()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(new ConflictException("User", "username", "bob").getMessage())
                .isEqualTo("User with username='bob' already exists");
        assertThat(new ConflictException().getMessage()).isEqualTo(ErrorCode.CONFLICT.defaultMessage());
    }

    @Test
    @DisplayName("ForbiddenException 两种构造器")
    void forbiddenExceptionConstructors() {
        assertThat(new ForbiddenException("no").errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(new ForbiddenException().getMessage()).isEqualTo(ErrorCode.FORBIDDEN.defaultMessage());
    }

    @Test
    @DisplayName("UnauthorizedException 两种构造器")
    void unauthorizedExceptionConstructors() {
        assertThat(new UnauthorizedException("no").errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(new UnauthorizedException().getMessage()).isEqualTo(ErrorCode.UNAUTHORIZED.defaultMessage());
    }

    @Test
    @DisplayName("ValidationException 三种构造器")
    void validationExceptionConstructors() {
        assertThat(new ValidationException("bad").getMessage()).isEqualTo("bad");
        assertThat(new ValidationException("age", "too low").getMessage())
                .isEqualTo("Validation failed for 'age': too low");
        assertThat(new ValidationException().getMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.defaultMessage());
    }
}
