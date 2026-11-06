// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA Specification utilities for applying data scope filtering.
 *
 * <p>Works together with {@link DataScopeContext} (set by {@link DataScopeAspect})
 * to automatically filter query results based on the current user's data permissions.
 *
 * <p>Usage in Repository or Service:
 * <pre>{@code
 * // Apply data scope to an existing specification
 * Specification<User> spec = Specification.where(baseSpec)
 *     .and(DataScopeSpecification.apply());
 * Page<User> result = userRepository.findAll(spec, pageable);
 *
 * // Or use directly
 * Page<User> result = userRepository.findAll(DataScopeSpecification.apply(), pageable);
 * }</pre>
 *
 * <p>Filtering logic:
 * <ul>
 *   <li>{@link DataScopeType#ALL} → no filtering (returns empty specification)</li>
 *   <li>{@link DataScopeType#DEPT_ONLY} → filter by single department ID</li>
 *   <li>{@link DataScopeType#DEPT_AND_CHILDREN} → filter by department ID set (including children)</li>
 *   <li>{@link DataScopeType#SELF_ONLY} → filter by creator/user ID</li>
 *   <li>{@link DataScopeType#CUSTOM} → filter by custom department ID set</li>
 * </ul>
 *
 * <p>If no {@link DataScopeContext} is set (e.g., called outside a @DataScope method),
 * no filtering is applied.
 *
 * @param <T> the entity type
 * @see DataScope
 * @see DataScopeContext
 * @see DataScopeAspect
 */
public final class DataScopeSpecification {

    private static final Logger log = LoggerFactory.getLogger(DataScopeSpecification.class);

    private DataScopeSpecification() {
        // Utility class
    }

    /**
     * Apply data scope filtering based on the current {@link DataScopeContext}.
     *
     * <p>Returns an empty specification (no filtering) when:
     * <ul>
     *   <li>No DataScopeContext is set</li>
     *   <li>The scope type is ALL</li>
     *   <li>The required field values are missing</li>
     * </ul>
     *
     * @param <T> the entity type
     * @return a specification that applies data scope filtering
     */
    public static <T> Specification<T> apply() {
        DataScopeContext context = DataScopeContext.get();
        if (context == null) {
            log.debug("No DataScopeContext set, returning empty specification");
            return empty();
        }

        log.debug(
                "Applying data scope: type={}, userId={}, deptId={}, deptIds={}",
                context.getScopeType(),
                context.getUserId(),
                context.getDeptId(),
                context.getDeptIds());

        return switch (context.getScopeType()) {
            case ALL -> empty();
            case DEPT_ONLY -> deptEquals(context.getDeptField(), context.getDeptId());
            case DEPT_AND_CHILDREN -> deptIn(context.getDeptField(), context.getDeptIds());
            case SELF_ONLY -> userEquals(context.getUserField(), context.getUserId());
            case CUSTOM -> deptIn(context.getDeptField(), context.getDeptIds());
        };
    }

    /**
     * Apply data scope filtering with a fallback specification.
     *
     * <p>If no DataScopeContext is set, the fallback specification is used instead.
     *
     * @param fallback the specification to use when no context is set
     * @param <T>      the entity type
     * @return a specification that applies data scope filtering or the fallback
     */
    public static <T> Specification<T> applyOrElse(Specification<T> fallback) {
        DataScopeContext context = DataScopeContext.get();
        if (context == null) {
            return fallback;
        }
        return apply();
    }

    /**
     * Check if data scope filtering is currently active.
     *
     * @return true if a DataScopeContext is set and filtering is required
     */
    public static boolean isActive() {
        DataScopeContext context = DataScopeContext.get();
        return context != null && !context.isAllData();
    }

    /**
     * Get the current data scope type, or null if not set.
     *
     * @return the current data scope type
     */
    public static DataScopeType currentScopeType() {
        DataScopeContext context = DataScopeContext.get();
        return context != null ? context.getScopeType() : null;
    }

    // ===== Private specification builders =====

    private static <T> Specification<T> empty() {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> null;
    }

    private static <T> Specification<T> deptEquals(String field, Long deptId) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (deptId == null) {
                log.warn("Department ID is null for DEPT_ONLY scope, returning no results");
                return cb.disjunction(); // Returns no results
            }
            return cb.equal(root.get(field), deptId);
        };
    }

    private static <T> Specification<T> deptIn(String field, Set<Long> deptIds) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (deptIds == null || deptIds.isEmpty()) {
                log.warn("Department IDs are empty for DEPT scope, returning no results");
                return cb.disjunction(); // Returns no results
            }
            return root.get(field).in(deptIds);
        };
    }

    private static <T> Specification<T> userEquals(String field, Long userId) {
        return (Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            if (userId == null) {
                log.warn("User ID is null for SELF_ONLY scope, returning no results");
                return cb.disjunction(); // Returns no results
            }
            return cb.equal(root.get(field), userId);
        };
    }
}
