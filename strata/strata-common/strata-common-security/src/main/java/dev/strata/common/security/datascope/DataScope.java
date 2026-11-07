// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for enabling data scope (row-level permission) filtering on a method.
 *
 * <p>When applied to a Controller or Service method, the data scope of the current user
 * is determined and stored in {@link DataScopeContext}, which can be used by
 * repositories or queries to filter results.
 *
 * <p>Usage:
 * <pre>{@code
 * // Method-level
 * @GetMapping("/users")
 * @DataScope(deptField = "dept_id", userField = "created_by")
 * public Page<User> listUsers(...) {
 *     // Data scope is automatically applied
 *     return userService.findAll(pageable);
 * }
 *
 * // Class-level (applies to all methods)
 * @RestController
 * @RequestMapping("/orders")
 * @DataScope(deptField = "dept_id")
 * public class OrderController { ... }
 * }</pre>
 *
 * <p>The actual filtering is done by:
 * <ul>
 *   <li>Hibernate Filter (if entity has {@code @FilterDef})</li>
 *   <li>JPA Specification (using {@link DataScopeContext})</li>
 *   <li>Custom query interceptor</li>
 * </ul>
 *
 * @see DataScopeContext
 * @see DataScopeType
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * The database column name for department ID.
     * Used to filter by department scope.
     *
     * @return the department column name (default: "dept_id")
     */
    String deptField() default "dept_id";

    /**
     * The database column name for creator/user ID.
     * Used to filter by self scope.
     *
     * @return the user column name (default: "created_by")
     */
    String userField() default "created_by";

    /**
     * Whether to automatically apply the data scope filter.
     * If false, the scope is only stored in context for manual use.
     *
     * @return true to auto-apply, false for manual use
     */
    boolean autoApply() default true;

    /**
     * Description of the data scope (for documentation/audit).
     *
     * @return the description
     */
    String description() default "";
}
