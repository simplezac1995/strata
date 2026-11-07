// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.spec;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

/**
 * Drives the predicate lambdas inside {@link Specs} by invoking {@link Specification#toPredicate} against
 * mocked criteria API objects. The existing {@code SpecsTest} only checked the skip/accumulate flags;
 * this class exercises every builder branch (eq/like/in/isNull/ge/le/lt and the empty conjunction).
 */
@SuppressWarnings({"unchecked", "varargs"})
class SpecsPredicateTest {

    @SuppressWarnings("unchecked")
    private final Root<Object> root = mock(Root.class);

    @SuppressWarnings("unchecked")
    private final CriteriaQuery<Object> query = mock(CriteriaQuery.class);

    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);

    @SuppressWarnings("unchecked")
    private final Path<Object> path = mock(Path.class);

    private void stubRoot() {
        when(root.get(any(String.class))).thenReturn(path);
    }

    @Test
    void eqAppliesEqual() {
        stubRoot();
        Predicate p = mock(Predicate.class);
        when(cb.equal(any(), any())).thenReturn(p);

        Specs.of(Object.class).eq("enabled", true).toPredicate().toPredicate(root, query, cb);

        verify(cb).equal(path, true);
    }

    @Test
    void likeAppliesLowerLike() {
        stubRoot();

        Specs.of(Object.class).like("name", "Widget").toPredicate().toPredicate(root, query, cb);

        verify(cb).like(any(), eq("%widget%"));
    }

    @Test
    void inAppliesIn() {
        stubRoot();

        Specs.of(Object.class).in("id", List.of(1, 2)).toPredicate().toPredicate(root, query, cb);

        verify(path).in(List.of(1, 2));
    }

    @Test
    void isNullAppliesWhenTrue() {
        stubRoot();

        Specs.of(Object.class).isNull("deleted", true).toPredicate().toPredicate(root, query, cb);

        verify(cb).isNull(path);
    }

    @Test
    void isNullSkipsWhenFalse() {
        stubRoot();

        Specs.of(Object.class).isNull("deleted", false).toPredicate().toPredicate(root, query, cb);

        verify(cb, org.mockito.Mockito.never()).isNull(any());
    }

    @Test
    void rangePredicatesApply() {
        stubRoot();

        Specs.of(Object.class)
                .ge("age", 10)
                .le("age", 20)
                .lt("age", 30)
                .toPredicate()
                .toPredicate(root, query, cb);

        verify(cb).greaterThanOrEqualTo(any(jakarta.persistence.criteria.Expression.class), any(Comparable.class));
        verify(cb).lessThanOrEqualTo(any(jakarta.persistence.criteria.Expression.class), any(Comparable.class));
        verify(cb).lessThan(any(jakarta.persistence.criteria.Expression.class), any(Comparable.class));
    }

    @Test
    void emptyYieldsConjunction() {
        stubRoot();

        Specs.of(Object.class).toPredicate().toPredicate(root, query, cb);

        verify(cb).conjunction();
    }
}
