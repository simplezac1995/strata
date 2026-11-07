// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.datascope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.jpa.domain.Specification;

class DataScopeSpecificationTest {

    private static final String DEPT_FIELD = "deptId";
    private static final String USER_FIELD = "creatorId";

    private Root<Object> root;
    private CriteriaQuery<Object> query;
    private CriteriaBuilder cb;
    private Path<Object> deptPath;
    private Path<Object> userPath;
    private Predicate deptEqual;
    private Predicate userEqual;
    private Predicate inPredicate;
    private Predicate disjunction;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        root = mock(Root.class);
        query = mock(CriteriaQuery.class);
        cb = mock(CriteriaBuilder.class);
        deptPath = mock(Path.class);
        userPath = mock(Path.class);
        deptEqual = mock(Predicate.class);
        userEqual = mock(Predicate.class);
        inPredicate = mock(Predicate.class);
        disjunction = mock(Predicate.class);

        lenient().when(root.get(DEPT_FIELD)).thenReturn(deptPath);
        lenient().when(root.get(USER_FIELD)).thenReturn(userPath);
        lenient().when(cb.disjunction()).thenReturn(disjunction);
        lenient().when(cb.equal(deptPath, 10L)).thenReturn(deptEqual);
        lenient().when(cb.equal(userPath, 7L)).thenReturn(userEqual);
        lenient().when(deptPath.in(anyCollection())).thenReturn(inPredicate);
    }

    @AfterEach
    void clearContext() {
        DataScopeContext.clear();
    }

    private Predicate toPredicate(Specification<Object> spec) {
        try {
            return spec.toPredicate(root, query, cb);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void noContextMeansNoFilter() {
        assertThat(toPredicate(DataScopeSpecification.apply())).isNull();
        assertThat(DataScopeSpecification.isActive()).isFalse();
        assertThat(DataScopeSpecification.currentScopeType()).isNull();
    }

    @Test
    void allScopeMeansNoFilter() {
        DataScopeContext.set(DataScopeContext.allData(1L, DEPT_FIELD, USER_FIELD));

        assertThat(toPredicate(DataScopeSpecification.apply())).isNull();
        assertThat(DataScopeSpecification.isActive()).isFalse();
        assertThat(DataScopeSpecification.currentScopeType()).isEqualTo(DataScopeType.ALL);
    }

    @Test
    void deptOnlyWithIdFiltersByEquality() {
        DataScopeContext.set(
                DataScopeContext.deptScoped(DataScopeType.DEPT_ONLY, 1L, 10L, Set.of(10L), DEPT_FIELD, USER_FIELD));

        assertThat(toPredicate(DataScopeSpecification.apply())).isSameAs(deptEqual);
        verify(cb).equal(deptPath, 10L);
        assertThat(DataScopeSpecification.isActive()).isTrue();
    }

    @Test
    void deptOnlyWithoutIdDeniesAll() {
        DataScopeContext.set(DataScopeContext.deptScoped(
                DataScopeType.DEPT_ONLY, 1L, null, Collections.emptySet(), DEPT_FIELD, USER_FIELD));

        assertThat(toPredicate(DataScopeSpecification.apply())).isSameAs(disjunction);
        verify(cb, never()).equal(any(), any());
    }

    @Test
    void deptAndChildrenFiltersByIn() {
        DataScopeContext.set(DataScopeContext.deptScoped(
                DataScopeType.DEPT_AND_CHILDREN, 1L, 5L, Set.of(5L, 6L), DEPT_FIELD, USER_FIELD));

        assertThat(toPredicate(DataScopeSpecification.apply())).isSameAs(inPredicate);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(deptPath).in(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(5L, 6L);
    }

    @Test
    void deptAndChildrenEmptyDeniesAll() {
        DataScopeContext.set(DataScopeContext.deptScoped(
                DataScopeType.DEPT_AND_CHILDREN, 1L, 5L, Collections.emptySet(), DEPT_FIELD, USER_FIELD));

        assertThat(toPredicate(DataScopeSpecification.apply())).isSameAs(disjunction);
        verify(deptPath, never()).in(anyCollection());
    }

    @Test
    void selfOnlyWithUserFiltersByEquality() {
        DataScopeContext.set(DataScopeContext.selfOnly(7L, DEPT_FIELD, USER_FIELD));

        assertThat(toPredicate(DataScopeSpecification.apply())).isSameAs(userEqual);
        verify(cb).equal(userPath, 7L);
    }

    @Test
    void selfOnlyWithoutUserDeniesAll() {
        DataScopeContext.set(DataScopeContext.selfOnly(null, DEPT_FIELD, USER_FIELD));

        assertThat(toPredicate(DataScopeSpecification.apply())).isSameAs(disjunction);
        verify(cb, never()).equal(any(), any());
    }

    @Test
    void customFiltersByDeptIn() {
        DataScopeContext.set(
                DataScopeContext.deptScoped(DataScopeType.CUSTOM, 1L, null, Set.of(9L, 10L), DEPT_FIELD, USER_FIELD));

        assertThat(toPredicate(DataScopeSpecification.apply())).isSameAs(inPredicate);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(deptPath).in(captor.capture());
        assertThat(captor.getValue()).containsExactlyInAnyOrder(9L, 10L);
    }

    @Test
    void applyOrElseUsesFallbackWhenNoContext() {
        Predicate fallbackPredicate = mock(Predicate.class);
        Specification<Object> fallback = (r, q, c) -> fallbackPredicate;

        assertThat(toPredicate(DataScopeSpecification.applyOrElse(fallback))).isSameAs(fallbackPredicate);
    }

    @Test
    void applyOrElseIgnoresFallbackWhenContextPresent() {
        Predicate fallbackPredicate = mock(Predicate.class);
        Specification<Object> fallback = (r, q, c) -> fallbackPredicate;
        DataScopeContext.set(DataScopeContext.selfOnly(7L, DEPT_FIELD, USER_FIELD));

        assertThat(toPredicate(DataScopeSpecification.applyOrElse(fallback))).isSameAs(userEqual);
    }
}
