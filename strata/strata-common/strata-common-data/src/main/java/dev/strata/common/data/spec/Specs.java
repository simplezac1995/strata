// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.spec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/**
 * Fluent JPA Specification builder. Null / blank conditions are skipped.
 *
 * <pre>{@code
 * Specs.of(User.class).eq("enabled", true).like("username", keyword).toPredicate()
 * }</pre>
 */
public final class Specs<T> {

    private final List<Specification<T>> parts = new ArrayList<>();

    private Specs() {}

    public static <T> Specs<T> of(Class<T> type) {
        return new Specs<>();
    }

    public Specs<T> eq(String attribute, Object value) {
        if (isSkipped(value)) {
            return this;
        }
        parts.add((root, query, cb) -> cb.equal(root.get(attribute), value));
        return this;
    }

    public Specs<T> like(String attribute, String value) {
        if (value == null || value.isBlank()) {
            return this;
        }
        String pattern = "%" + value.trim().toLowerCase() + "%";
        parts.add((root, query, cb) -> cb.like(cb.lower(root.get(attribute).as(String.class)), pattern));
        return this;
    }

    public Specs<T> in(String attribute, Collection<?> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }
        parts.add((root, query, cb) -> root.get(attribute).in(values));
        return this;
    }

    public Specs<T> isNull(String attribute, Boolean apply) {
        if (!Boolean.TRUE.equals(apply)) {
            return this;
        }
        parts.add((root, query, cb) -> cb.isNull(root.get(attribute)));
        return this;
    }

    @SuppressWarnings("unchecked")
    public Specs<T> ge(String attribute, Comparable<?> value) {
        if (value == null) {
            return this;
        }
        parts.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get(attribute), unchecked(value)));
        return this;
    }

    @SuppressWarnings("unchecked")
    public Specs<T> le(String attribute, Comparable<?> value) {
        if (value == null) {
            return this;
        }
        parts.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get(attribute), unchecked(value)));
        return this;
    }

    @SuppressWarnings("unchecked")
    public Specs<T> lt(String attribute, Comparable<?> value) {
        if (value == null) {
            return this;
        }
        parts.add((root, query, cb) -> cb.lessThan(root.get(attribute), unchecked(value)));
        return this;
    }

    /** Docs alias: returns a Spring Data {@link Specification}. */
    public Specification<T> toPredicate() {
        return toSpecification();
    }

    public Specification<T> toSpecification() {
        Specification<T> result = null;
        for (Specification<T> part : parts) {
            result = result == null ? part : result.and(part);
        }
        return result == null ? (root, query, cb) -> cb.conjunction() : result;
    }

    int conditionCount() {
        return parts.size();
    }

    private static boolean isSkipped(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String text) {
            return text.isBlank();
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparable unchecked(Comparable<?> value) {
        return value;
    }
}
