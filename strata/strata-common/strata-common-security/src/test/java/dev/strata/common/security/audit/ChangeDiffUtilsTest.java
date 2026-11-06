// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.audit;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.common.security.audit.ChangeDiffUtils.FieldChange;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ChangeDiffUtils}: field-level diffing, ignored fields, null/type
 * boundaries, and JSON/summary rendering.
 */
@DisplayName("ChangeDiffUtils 变更对比")
class ChangeDiffUtilsTest {

    /** Sample bean with tracked and ignored fields. */
    static class User {
        String name;
        Integer age;
        String id;
        String createdAt;

        User(String name, Integer age, String id, String createdAt) {
            this.name = name;
            this.age = age;
            this.id = id;
            this.createdAt = createdAt;
        }
    }

    @Test
    @DisplayName("两对象无差异时返回空 diff")
    void identicalObjectsProduceEmptyDiff() {
        User a = new User("bob", 30, "1", "now");
        User b = new User("bob", 30, "1", "now");

        assertThat(ChangeDiffUtils.diff(a, b)).isEmpty();
        assertThat(ChangeDiffUtils.hasChanges(a, b)).isFalse();
    }

    @Test
    @DisplayName("字段变化时记录 old->new 且跳过忽略字段")
    void changedFieldIsReportedAndIgnoredFieldsSkipped() {
        User old = new User("bob", 30, "1", "now");
        User now = new User("alice", 31, "1", "later");

        Map<String, FieldChange> diff = ChangeDiffUtils.diff(old, now);

        assertThat(diff).containsOnlyKeys("name", "age");
        assertThat(diff.get("name").getOldValue()).isEqualTo("bob");
        assertThat(diff.get("name").getNewValue()).isEqualTo("alice");
        assertThat(ChangeDiffUtils.hasChanges(old, now)).isTrue();
    }

    @Test
    @DisplayName("old 为 null 时记录 __object__ 变化")
    void nullOldValueIsCaptured() {
        Map<String, FieldChange> diff = ChangeDiffUtils.diff(null, "new");

        assertThat(diff).containsKey("__object__");
        assertThat(diff.get("__object__").getOldValue()).isNull();
        assertThat(diff.get("__object__").getNewValue()).isEqualTo("new");
    }

    @Test
    @DisplayName("两者都为 null 时返回空")
    void bothNullProduceEmptyDiff() {
        assertThat(ChangeDiffUtils.diff(null, null)).isEmpty();
    }

    @Test
    @DisplayName("类型不同时返回空 diff")
    void differentTypesProduceEmptyDiff() {
        assertThat(ChangeDiffUtils.diff("a", 1)).isEmpty();
    }

    @Test
    @DisplayName("自定义忽略字段生效")
    void customIgnoredFieldsAreSkipped() {
        User old = new User("bob", 30, "1", "now");
        User now = new User("bob", 99, "1", "now");

        Map<String, FieldChange> diff = ChangeDiffUtils.diff(old, now, List.of("age"));

        assertThat(diff).doesNotContainKey("age");
    }

    @Test
    @DisplayName("toJson 空 diff 返回 {}")
    void toJsonEmptyReturnsBraces() {
        assertThat(ChangeDiffUtils.toJson(Map.of())).isEqualTo("{}");
        assertThat(ChangeDiffUtils.toJson(null)).isEqualTo("{}");
    }

    @Test
    @DisplayName("toJson 非空 diff 产出 JSON")
    void toJsonNonEmptyProducesJson() {
        Map<String, FieldChange> diff = Map.of("name", new FieldChange("a", "b"));

        String json = ChangeDiffUtils.toJson(diff);

        assertThat(json).contains("\"name\"");
    }

    @Test
    @DisplayName("toSummary 空与非空")
    void toSummaryEmptyAndNonEmpty() {
        assertThat(ChangeDiffUtils.toSummary(Map.of())).isEqualTo("No changes");
        assertThat(ChangeDiffUtils.toSummary(null)).isEqualTo("No changes");

        Map<String, FieldChange> diff = Map.of("name", new FieldChange("a", "b"), "age", new FieldChange(1, 2));
        String summary = ChangeDiffUtils.toSummary(diff);
        assertThat(summary).startsWith("Changed 2 field(s):");
        assertThat(summary).contains("name").contains("age");
    }

    @Test
    @DisplayName("FieldChange toString")
    void fieldChangeToString() {
        assertThat(new FieldChange("a", "b").toString()).isEqualTo("a -> b");
    }
}
