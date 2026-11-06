// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.tenant.TenantContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

/**
 * Verifies the row-level tenant scoping helpers in {@link TenantScopeSupport}:
 * the switch (rowLevelEnabled) and the tenant context together decide whether a
 * {@code tenant_id} restriction is applied or the query stays unscoped.
 */
@SuppressWarnings({"unchecked", "varargs"})
class TenantScopeSupportTest {

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void disabledRowLevelNeverRestricts() {
        Specification<Object> spec = TenantScopeSupport.equalsTenantId(false, "tenantId");
        Predicate predicate = toPredicate(spec);

        assertThat(predicate).isNull();
        verify(cb).conjunction();
        verify(cb, never()).equal(any(Expression.class), any(Object.class));
    }

    @Test
    void enabledButNoTenantInContextRestrictsNothing() {
        Specification<Object> spec = TenantScopeSupport.equalsTenantId(true, "tenantId");
        Predicate predicate = toPredicate(spec);

        assertThat(predicate).isNull();
        verify(cb).conjunction();
        verify(cb, never()).equal(any(Expression.class), any(Object.class));
    }

    @Test
    void enabledWithTenantAddsEqualityRestriction() {
        TenantContext.setTenantId(42L);
        Specification<Object> spec = TenantScopeSupport.equalsTenantId(true, "tenantId");
        Predicate predicate = toPredicate(spec);

        assertThat(predicate).isNotNull();
        verify(cb).equal(root.get("tenantId"), 42L);
        verify(cb, never()).conjunction();
    }

    @Test
    void createWithDisabledRowLevelSkipsSetter() {
        AtomicLong setter = new AtomicLong(-1);
        TenantContext.setTenantId(7L);

        TenantScopeSupport.applyTenantIdOnCreate(false, setter::set);

        assertThat(setter.get()).isEqualTo(-1);
    }

    @Test
    void createWithEnabledAndTenantSetsTenantId() {
        AtomicLong setter = new AtomicLong(-1);
        TenantContext.setTenantId(7L);

        TenantScopeSupport.applyTenantIdOnCreate(true, setter::set);

        assertThat(setter.get()).isEqualTo(7L);
    }

    @Test
    void createWithEnabledButNoTenantSkipsSetter() {
        AtomicLong setter = new AtomicLong(-1);
        TenantScopeSupport.applyTenantIdOnCreate(true, setter::set);
        assertThat(setter.get()).isEqualTo(-1);
    }

    // ------------------------------------------------------------------

    private final Root<Object> root = mock(Root.class);
    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    @SuppressWarnings("unchecked")
    private Predicate toPredicate(Specification<Object> spec) {
        CriteriaQuery<Object> query = mock(CriteriaQuery.class);
        Path<Object> path = mock(Path.class);
        Expression<Object> literal42 = mock(Expression.class);
        Predicate predicate = mock(Predicate.class);

        when(root.get("tenantId")).thenReturn(path);
        when(cb.equal(path, 42L)).thenReturn(predicate);
        when(cb.conjunction()).thenReturn(null);

        return spec.toPredicate(root, query, cb);
    }
}
