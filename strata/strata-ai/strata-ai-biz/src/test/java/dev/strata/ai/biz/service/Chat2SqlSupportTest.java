// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Chat2SqlSupport SQL 守卫与图表推断")
class Chat2SqlSupportTest {

    @Test
    @DisplayName("系统提示注入 schemaHint 或默认 schema")
    void prompt_includes_schema() {
        String withHint = Chat2SqlSupport.buildSystemPrompt("custom schema");
        assertThat(withHint).contains("custom schema");
        String without = Chat2SqlSupport.buildSystemPrompt(null);
        assertThat(without).contains("live schema");
        assertThat(without).contains("PostgreSQL").contains("INTERVAL '7 days'");
    }

    @Test
    @DisplayName("从带 markdown 围栏的回复里提取第一个 SELECT")
    void extract_select_strips_fence_and_prose() {
        String reply = "Here is your query:\n```sql\nSELECT id, name FROM sys_user WHERE id = 1;\n```\nDone.";
        assertThat(Chat2SqlSupport.extractSelect(reply)).isEqualTo("SELECT id, name FROM sys_user WHERE id = 1");
    }

    @Test
    @DisplayName("回复里没有 SELECT 时抛异常")
    void extract_select_missing_throws() {
        assertThatThrownBy(() -> Chat2SqlSupport.extractSelect("I cannot help"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("只读 SELECT 通过，写操作被拒")
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
    @DisplayName("图表推断：时间列+2数值列→line，单数值列→bar，否则 null")
    void chart_suggestion_rules() {
        assertThat(Chat2SqlSupport.suggestChart(List.of("day", "amount", "cnt")))
                .isEqualTo("line");
        assertThat(Chat2SqlSupport.suggestChart(List.of("role", "cnt"))).isEqualTo("bar");
        assertThat(Chat2SqlSupport.suggestChart(List.of("day", "amount"))).isEqualTo("bar");
        assertThat(Chat2SqlSupport.suggestChart(List.of("a", "b"))).isNull();
    }
}
