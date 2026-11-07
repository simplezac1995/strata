// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking methods that require security audit logging.
 *
 * <p>Apply this annotation to methods that perform sensitive operations to automatically
 * log them for security audit purposes. The audit log includes user identity, operation
 * details, timestamp, and IP address.
 *
 * <p>Usage:
 * <pre>{@code
 * @AuditLog(action = "USER_CREATE", description = "Create new user")
 * public UserDto createUser(UserCreateRequest request) { ... }
 *
 * @AuditLog(action = "PERMISSION_CHANGE", description = "Change user permissions", sensitive = true)
 * public void updatePermissions(Long userId, List<String> permissions) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /**
     * Action identifier for the audit log.
     */
    String action();

    /**
     * Human-readable description of the operation.
     */
    String description() default "";

    /**
     * Whether this operation is sensitive and should be logged with additional detail.
     */
    boolean sensitive() default false;

    /**
     * Whether to log the method parameters.
     */
    boolean logParameters() default true;

    /**
     * Whether to log the return value.
     */
    boolean logReturnValue() default false;

    /**
     * Category for grouping audit logs.
     */
    String category() default "GENERAL";
}
