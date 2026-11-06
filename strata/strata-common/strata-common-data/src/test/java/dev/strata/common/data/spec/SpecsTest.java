// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.data.spec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

class SpecsTest {

    @Test
    void skipsNullAndBlankConditions() {
        Specs<Object> specs = Specs.of(Object.class)
                .eq("enabled", null)
                .eq("sku", " ")
                .like("name", null)
                .like("name", "")
                .in("id", null);

        assertEquals(0, specs.conditionCount());
        assertNotNull(specs.toPredicate());
    }

    @Test
    void accumulatesNonNullConditions() {
        Specs<Object> specs = Specs.of(Object.class)
                .eq("enabled", true)
                .like("name", " Widget ")
                .eq("sku", "SKU-1");

        assertEquals(3, specs.conditionCount());
        Specification<Object> specification = specs.toPredicate();
        assertNotNull(specification);
    }

    @Test
    void toPredicateAliasMatchesToSpecification() {
        Specs<Object> specs = Specs.of(Object.class).eq("enabled", true);
        assertEquals(specs.toSpecification().getClass(), specs.toPredicate().getClass());
    }
}
