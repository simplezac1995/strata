// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ScimFilterTest {

    private final Map<String, Object> user = Map.of(
            "userName",
            "Alice",
            "active",
            true,
            "emails",
            List.of(Map.of("value", "alice@example.com", "primary", true)),
            "name",
            Map.of("formatted", "Alice A"),
            "age",
            30);

    private boolean matches(String filter) {
        return ScimFilter.parse(filter).test(user);
    }

    @Test
    void eqAndNestedPath() {
        assertThat(matches("userName eq \"alice\"")).isTrue();
        assertThat(matches("emails.value eq \"alice@example.com\"")).isTrue();
        assertThat(matches("active eq true")).isTrue();
        assertThat(matches("userName eq \"bob\"")).isFalse();
    }

    @Test
    void comparisonOperators() {
        assertThat(matches("userName co \"lic\"")).isTrue();
        assertThat(matches("userName sw \"Ali\"")).isTrue();
        assertThat(matches("userName ew \"ICE\"")).isTrue();
        assertThat(matches("emails.value pr")).isTrue();
        assertThat(matches("phone pr")).isFalse();
        assertThat(matches("age gt 18")).isTrue();
        assertThat(matches("age lt 18")).isFalse();
    }

    @Test
    void andOrNotAndParentheses() {
        assertThat(matches("userName eq \"alice\" and active eq true")).isTrue();
        assertThat(matches("userName eq \"bob\" or active eq true")).isTrue();
        assertThat(matches("not (userName eq \"bob\")")).isTrue();
        assertThat(matches("(userName eq \"bob\" or userName eq \"alice\") and active eq true"))
                .isTrue();
    }

    @Test
    void invalidFilterRejected() {
        assertThatThrownBy(() -> ScimFilter.parse("userName co")).isInstanceOf(ScimException.class);
        assertThatThrownBy(() -> ScimFilter.parse("userName eq")).isInstanceOf(ScimException.class);
        assertThatThrownBy(() -> ScimFilter.parse("(userName eq \"a\"")).isInstanceOf(ScimException.class);
    }
}
