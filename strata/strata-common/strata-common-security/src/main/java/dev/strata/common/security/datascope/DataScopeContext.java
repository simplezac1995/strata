// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

import java.util.Collections;
import java.util.Set;

/**
 * Thread-local context for the current user's data scope.
 *
 * <p>Set by {@link DataScopeAspect} and accessible throughout the request lifecycle.
 * Used by repositories, services, and queries to filter data based on the user's permissions.
 *
 * <p>Usage:
 * <pre>{@code
 * DataScopeContext context = DataScopeContext.get();
 * if (context.isAllData()) {
 *     // No filtering needed
 * } else if (context.isDeptFiltered()) {
 *     Set<Long> deptIds = context.getDeptIds();
 *     // Filter by deptIds
 * } else if (context.isSelfFiltered()) {
 *     Long userId = context.getUserId();
 *     // Filter by userId
 * }
 * }</pre>
 *
 * <p>Note: This context is cleared after each request by the aspect.
 */
public final class DataScopeContext {

    private static final ThreadLocal<DataScopeContext> CONTEXT = new ThreadLocal<>();

    private final DataScopeType scopeType;
    private final Long userId;
    private final Long deptId;
    private final Set<Long> deptIds;
    private final String deptField;
    private final String userField;

    private DataScopeContext(
            DataScopeType scopeType, Long userId, Long deptId, Set<Long> deptIds, String deptField, String userField) {
        this.scopeType = scopeType;
        this.userId = userId;
        this.deptId = deptId;
        this.deptIds = deptIds != null ? Collections.unmodifiableSet(deptIds) : Collections.emptySet();
        this.deptField = deptField;
        this.userField = userField;
    }

    /**
     * Create a context for ALL data scope (no filtering).
     */
    public static DataScopeContext allData(Long userId, String deptField, String userField) {
        return new DataScopeContext(DataScopeType.ALL, userId, null, null, deptField, userField);
    }

    /**
     * Create a context for department-based data scope.
     */
    public static DataScopeContext deptScoped(
            DataScopeType type, Long userId, Long deptId, Set<Long> deptIds, String deptField, String userField) {
        return new DataScopeContext(type, userId, deptId, deptIds, deptField, userField);
    }

    /**
     * Create a context for self-only data scope.
     */
    public static DataScopeContext selfOnly(Long userId, String deptField, String userField) {
        return new DataScopeContext(DataScopeType.SELF_ONLY, userId, null, null, deptField, userField);
    }

    /**
     * Get the current data scope context.
     *
     * @return the current context, or null if not set
     */
    public static DataScopeContext get() {
        return CONTEXT.get();
    }

    /**
     * Set the current data scope context.
     * Called by {@link DataScopeAspect}.
     */
    static void set(DataScopeContext context) {
        CONTEXT.set(context);
    }

    /**
     * Clear the current context.
     * Called by {@link DataScopeAspect} after method completion.
     */
    static void clear() {
        CONTEXT.remove();
    }

    /**
     * Check if the current user has access to all data (no filtering needed).
     */
    public boolean isAllData() {
        return scopeType != null && scopeType.isAllData();
    }

    /**
     * Check if the current scope requires department-based filtering.
     */
    public boolean isDeptFiltered() {
        return scopeType != null && scopeType.requiresDeptFilter();
    }

    /**
     * Check if the current scope requires self-based filtering.
     */
    public boolean isSelfFiltered() {
        return scopeType != null && scopeType.requiresSelfFilter();
    }

    public DataScopeType getScopeType() {
        return scopeType;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getDeptId() {
        return deptId;
    }

    public Set<Long> getDeptIds() {
        return deptIds;
    }

    public String getDeptField() {
        return deptField;
    }

    public String getUserField() {
        return userField;
    }
}
