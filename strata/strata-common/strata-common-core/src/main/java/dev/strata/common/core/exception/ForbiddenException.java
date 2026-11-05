// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.exception;

/**
 * Exception thrown when an authenticated user lacks permission for an operation.
 *
 * <p>Use this for business-level authorization checks, such as tenant isolation,
 * resource ownership, or feature flag restrictions. Framework-level access denials
 * are handled by Spring Security.
 *
 * <p>Example:
 * <pre>{@code
 * if (!resource.getOwnerId().equals(currentUser.getId())) {
 *     throw new ForbiddenException("You do not have permission to modify this resource");
 * }
 * }</pre>
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }
}
