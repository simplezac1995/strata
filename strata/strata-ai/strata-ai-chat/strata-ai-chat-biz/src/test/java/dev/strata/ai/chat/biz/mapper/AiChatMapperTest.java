// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.ai.chat.api.dto.AiChatMessageDto;
import dev.strata.ai.chat.api.dto.AiChatSessionDto;
import dev.strata.ai.chat.api.entity.AiChatMessage;
import dev.strata.ai.chat.api.entity.AiChatSession;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AiChatMapper 接口契约单元测试：覆盖 session==null、modelId 三元表达式的 null/非 null 分支，不启动 Spring
 * 容器。
 */
class AiChatMapperTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private final AiChatMapper mapper = new AiChatMapperImpl();

    @Test
    @DisplayName("toSessionDto(null) 返回 null")
    void sessionNull() {
        assertThat(mapper.toSessionDto(null)).isNull();
    }

    @Test
    @DisplayName("toSessionDto modelId 非空时转换为字符串")
    void sessionModelIdNonNull() {
        AiChatSession session = new AiChatSession();
        session.setId(11L);
        session.setTitle("标题");
        session.setModelId(99L);
        session.setUserId(5L);
        session.setCreatedAt(NOW);
        AiChatSessionDto dto = mapper.toSessionDto(session);
        assertThat(dto.id()).isEqualTo("11");
        assertThat(dto.title()).isEqualTo("标题");
        assertThat(dto.modelId()).isEqualTo("99");
        assertThat(dto.userId()).isEqualTo("5");
        assertThat(dto.createdAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("toSessionDto modelId 为 null 时目标 modelId 为 null（三元表达式 null 分支）")
    void sessionModelIdNull() {
        AiChatSession session = new AiChatSession();
        session.setId(11L);
        session.setTitle("标题");
        session.setModelId(null);
        session.setUserId(5L);
        AiChatSessionDto dto = mapper.toSessionDto(session);
        assertThat(dto.modelId()).isNull();
        assertThat(dto.userId()).isEqualTo("5");
    }

    @Test
    @DisplayName("toMessageDto(null) 返回 null")
    void messageNull() {
        assertThat(mapper.toMessageDto(null)).isNull();
    }

    @Test
    @DisplayName("toMessageDto 全字段映射")
    void messageFull() {
        AiChatMessage message = new AiChatMessage();
        message.setId(22L);
        message.setSessionId(11L);
        message.setRole("user");
        message.setContent("你好");
        message.setCreatedAt(NOW);
        AiChatMessageDto dto = mapper.toMessageDto(message);
        assertThat(dto.id()).isEqualTo("22");
        assertThat(dto.sessionId()).isEqualTo("11");
        assertThat(dto.role()).isEqualTo("user");
        assertThat(dto.content()).isEqualTo("你好");
        assertThat(dto.createdAt()).isEqualTo(NOW);
    }
}
