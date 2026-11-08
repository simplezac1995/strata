// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

import java.util.Set;

/**
 * Provider interface for resolving a user's data scope.
 *
 * <p>Implementations should retrieve the user's role-based data scope from
 * the security context or database.
 *
 * <p>Usage:
 * <pre>{@code
 * @Component
 * public class DefaultDataScopeProvider implements DataScopeProvider {
 *     @Override
 *     public DataScopeType resolveScope(Long userId) {
 *         // Get user's highest-privilege role's data scope
 *         return DataScopeType.DEPT_AND_CHILDREN;
 *     }
 *
 *     @Override
 *     public Set<Long> resolveCustomDeptIds(Long userId) {
 *         // Get custom department IDs for CUSTOM scope
 *         return Set.of(1L, 2L, 3L);
 *     }
 * }
 * }</pre>
 */
public interface DataScopeProvider {

    /**
     * Resolve the data scope type for the given user.
     *
     * @param userId the user ID
     * @return the data scope type
     */
    DataScopeType resolveScope(Long userId);

    /**
     * Resolve the custom department IDs for the given user.
     * Only used when the scope type is {@link DataScopeType#CUSTOM}.
     *
     * @param userId the user ID
     * @return the set of custom department IDs
     */
    Set<Long> resolveCustomDeptIds(Long userId);

    /**
     * Resolve the department ID for the given user.
     * Used for DEPT_ONLY and DEPT_AND_CHILDREN scopes.
     *
     * @param userId the user ID
     * @return the user's department ID
     */
    Long resolveDeptId(Long userId);

    /**
     * Resolve all descendant department IDs for the given department.
     * Used for DEPT_AND_CHILDREN scope.
     *
     * @param deptId the department ID
     * @return the set of department IDs including the given one and all descendants
     */
    Set<Long> resolveDeptAndChildren(Long deptId);

    /**
     * Get the current user's ID from the security context.
     *
     * @return the current user ID, or null if not authenticated
     */
    Long getCurrentUserId();
}
