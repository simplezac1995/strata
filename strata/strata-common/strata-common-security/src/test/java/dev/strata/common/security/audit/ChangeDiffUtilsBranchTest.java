// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.audit;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.common.security.audit.ChangeDiffUtils.FieldChange;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Branch arms of {@link ChangeDiffUtils} not in {@link ChangeDiffUtilsTest}: the {@code newValue ==
 * null} object-replacement arm, superclass field traversal, static-field skip, null-vs-nonnull value
 * comparison, and every {@code safeToString} type arm (Boolean / Enum / nested object JSON).
 */
class ChangeDiffUtilsBranchTest {

    static class Base {
        String baseField = "base";
    }

    enum Status {
        ON,
        OFF
    }

    static class Nested {
        public String v;

        Nested() {}

        Nested(String v) {
            this.v = v;
        }
    }

    static class Props extends Base {
        static String STATIC_IGNORED = "should-not-appear";
        String name;
        int age;
        boolean active;
        Status status;
        Nested nested;

        Props(String name, int age, boolean active, Status status, Nested nested) {
            this.name = name;
            this.age = age;
            this.active = active;
            this.status = status;
            this.nested = nested;
        }
    }

    @Test
    void newNullCapturesObjectReplacement() {
        Props old = new Props("a", 1, true, Status.ON, new Nested("x"));

        Map<String, FieldChange> diff = ChangeDiffUtils.diff(old, null);

        assertThat(diff).containsKey("__object__");
        // oldValue 为 Props（非 String/Number/Boolean/Enum），走 JSON 序列化；构造器约束下回退 toString
        assertThat(diff.get("__object__").getOldValue()).isNotNull();
        assertThat(diff.get("__object__").getNewValue()).isNull();
    }

    @Test
    void nullVsNonNullFieldRecordsChange() {
        Props old = new Props("alice", 1, true, Status.ON, new Nested("x"));
        Props now = new Props(null, 1, true, Status.ON, new Nested("x"));

        Map<String, FieldChange> diff = ChangeDiffUtils.diff(old, now);

        assertThat(diff).containsKey("name");
        assertThat(diff.get("name").getOldValue()).isEqualTo("alice");
        assertThat(diff.get("name").getNewValue()).isNull();
    }

    @Test
    void booleanAndEnumBranches() {
        Props old = new Props("a", 1, false, Status.OFF, new Nested("x"));
        Props now = new Props("a", 1, true, Status.ON, new Nested("x"));

        Map<String, FieldChange> diff = ChangeDiffUtils.diff(old, now);

        assertThat(diff.get("active").getOldValue()).isEqualTo("false");
        assertThat(diff.get("active").getNewValue()).isEqualTo("true");
        assertThat(diff.get("status").getOldValue()).isEqualTo("OFF");
        assertThat(diff.get("status").getNewValue()).isEqualTo("ON");
    }

    @Test
    void nestedObjectJsonAndInheritedFields() {
        Props old = new Props("a", 1, true, Status.ON, new Nested("x"));
        Props now = new Props("a", 1, true, Status.ON, new Nested("y"));

        Map<String, FieldChange> diff = ChangeDiffUtils.diff(old, now);

        // 嵌套对象非 String/Number/Boolean/Enum -> writeValueAsString
        assertThat(diff.get("nested").getNewValue()).isEqualTo("{\"v\":\"y\"}");
        // 静态字段被跳过（不在结果中），继承字段 baseField 未变化故不出现
        assertThat(diff).doesNotContainKey("STATIC_IGNORED");
    }
}
