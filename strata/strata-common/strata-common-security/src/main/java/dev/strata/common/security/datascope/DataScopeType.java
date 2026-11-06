// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

/**
 * Data scope types for row-level data permission.
 *
 * <p>Determines which records a user can access based on their role and department.
 *
 * <p>Scope hierarchy (from most permissive to most restrictive):
 * <ol>
 *   <li>{@link #ALL} - All data (administrators)</li>
 *   <li>{@link #DEPT_AND_CHILDREN} - Current department and all sub-departments</li>
 *   <li>{@link #DEPT_ONLY} - Current department only</li>
 *   <li>{@link #SELF_ONLY} - Only data created by the current user</li>
 *   <li>{@link #CUSTOM} - Custom department list (defined per role)</li>
 * </ol>
 */
public enum DataScopeType {

    /**
     * All data - no filtering applied.
     * Typically assigned to administrators.
     */
    ALL("全部数据"),

    /**
     * Current department and all sub-departments.
     * Includes data from the user's department and all descendant departments.
     */
    DEPT_AND_CHILDREN("本部门及以下"),

    /**
     * Current department only.
     * Only includes data from the user's immediate department.
     */
    DEPT_ONLY("仅本部门"),

    /**
     * Only data created by the current user.
     * The most restrictive scope.
     */
    SELF_ONLY("仅本人"),

    /**
     * Custom department list.
     * Departments are defined per-role via {@code customDeptIds}.
     */
    CUSTOM("自定义部门");

    private final String description;

    DataScopeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this scope allows access to all data (no filtering needed).
     */
    public boolean isAllData() {
        return this == ALL;
    }

    /**
     * Check if this scope requires department-based filtering.
     */
    public boolean requiresDeptFilter() {
        return this == DEPT_AND_CHILDREN || this == DEPT_ONLY || this == CUSTOM;
    }

    /**
     * Check if this scope requires user-based filtering.
     */
    public boolean requiresSelfFilter() {
        return this == SELF_ONLY;
    }
}
