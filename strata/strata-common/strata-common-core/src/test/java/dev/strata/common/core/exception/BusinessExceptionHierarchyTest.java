// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/**
 * Unit tests for the exception hierarchy and {@link ErrorCode} metadata.
 */
class BusinessExceptionHierarchyTest {

    @Test
    void errorCodeMetadata() {
        ErrorCode code = ErrorCode.NOT_FOUND;

        assertThat(code.code()).isEqualTo(40400);
        assertThat(code.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(code.defaultMessage()).isEqualTo("Resource not found");
    }

    @Test
    void businessExceptionUsesDefaultMessage() {
        BusinessException ex = new BusinessException(ErrorCode.BAD_REQUEST);

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("Bad request");
    }

    @Test
    void businessExceptionCustomMessage() {
        BusinessException ex = new BusinessException(ErrorCode.BAD_REQUEST, "boom");

        assertThat(ex.getMessage()).isEqualTo("boom");
    }

    @Test
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
    void conflictExceptionConstructors() {
        assertThat(new ConflictException("dup").errorCode()).isEqualTo(ErrorCode.CONFLICT);
        assertThat(new ConflictException("User", "username", "bob").getMessage())
                .isEqualTo("User with username='bob' already exists");
        assertThat(new ConflictException().getMessage()).isEqualTo(ErrorCode.CONFLICT.defaultMessage());
    }

    @Test
    void forbiddenExceptionConstructors() {
        assertThat(new ForbiddenException("no").errorCode()).isEqualTo(ErrorCode.FORBIDDEN);
        assertThat(new ForbiddenException().getMessage()).isEqualTo(ErrorCode.FORBIDDEN.defaultMessage());
    }

    @Test
    void unauthorizedExceptionConstructors() {
        assertThat(new UnauthorizedException("no").errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
        assertThat(new UnauthorizedException().getMessage()).isEqualTo(ErrorCode.UNAUTHORIZED.defaultMessage());
    }

    @Test
    void validationExceptionConstructors() {
        assertThat(new ValidationException("bad").getMessage()).isEqualTo("bad");
        assertThat(new ValidationException("age", "too low").getMessage())
                .isEqualTo("Validation failed for 'age': too low");
        assertThat(new ValidationException().getMessage()).isEqualTo(ErrorCode.VALIDATION_FAILED.defaultMessage());
    }
}
