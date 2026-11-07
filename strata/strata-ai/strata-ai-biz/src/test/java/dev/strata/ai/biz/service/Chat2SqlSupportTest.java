// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class Chat2SqlSupportTest {

    @Test
    void prompt_includes_schema() {
        String withHint = Chat2SqlSupport.buildSystemPrompt("custom schema");
        assertThat(withHint).contains("custom schema");
        String without = Chat2SqlSupport.buildSystemPrompt(null);
        assertThat(without).contains("live schema");
        assertThat(without).contains("PostgreSQL").contains("INTERVAL '7 days'");
    }

    @Test
    void extract_select_strips_fence_and_prose() {
        String reply = "Here is your query:\n```sql\nSELECT id, name FROM sys_user WHERE id = 1;\n```\nDone.";
        assertThat(Chat2SqlSupport.extractSelect(reply)).isEqualTo("SELECT id, name FROM sys_user WHERE id = 1");
    }

    @Test
    void extract_select_missing_throws() {
        assertThatThrownBy(() -> Chat2SqlSupport.extractSelect("I cannot help"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void read_only_guard_blocks_writes() {
        assertThat(Chat2SqlSupport.isReadOnlySelect("SELECT * FROM sys_user")).isTrue();
        assertThat(Chat2SqlSupport.isReadOnlySelect("WITH active AS (SELECT 1) SELECT * FROM active"))
                .isTrue();
        assertThat(Chat2SqlSupport.isReadOnlySelect("DELETE FROM sys_user")).isFalse();
        assertThat(Chat2SqlSupport.isReadOnlySelect("SELECT * FROM sys_user; DROP TABLE sys_user"))
                .isFalse();
        assertThat(Chat2SqlSupport.isReadOnlySelect("UPDATE sys_user SET name='x'"))
                .isFalse();
        assertThat(Chat2SqlSupport.isReadOnlySelect("")).isFalse();
    }

    @Test
    void chart_suggestion_rules() {
        assertThat(Chat2SqlSupport.suggestChart(List.of("day", "amount", "cnt")))
                .isEqualTo("line");
        assertThat(Chat2SqlSupport.suggestChart(List.of("role", "cnt"))).isEqualTo("bar");
        assertThat(Chat2SqlSupport.suggestChart(List.of("day", "amount"))).isEqualTo("bar");
        assertThat(Chat2SqlSupport.suggestChart(List.of("a", "b"))).isNull();
    }
}
