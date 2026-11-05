// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.request.AiChatSessionCreateRequest;
import dev.strata.ai.chat.api.dto.AiChatSessionDto;
import dev.strata.ai.chat.api.entity.AiChatSession;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatAppService 会话管理")
class AiChatAppServiceTest extends AiChatAppServiceTestBase {

    @Nested
    @DisplayName("会话查询/创建/删除")
    class SessionCrud {

        @Test
        @DisplayName("listSessions 按当前用户倒序映射（限量最近 100 条）")
        void listSessions_scoped_to_current_user() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findTop100ByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(s));
            when(mapper.toSessionDto(s)).thenReturn(new AiChatSessionDto("1", "t", "10", "7", null));

            List<AiChatSessionDto> result = service.listSessions();

            assertThat(result).hasSize(1);
            verify(sessionRepository).findTop100ByUserIdOrderByCreatedAtDesc(USER_ID);
        }

        @Test
        @DisplayName("createSession 显式指定 modelId 时解析并保存")
        void createSession_with_explicit_model() {
            AiChatSessionCreateRequest request = new AiChatSessionCreateRequest("我的会话", "42");
            when(mapper.toSessionDto(any(AiChatSession.class))).thenAnswer(inv -> {
                AiChatSession s = inv.getArgument(0);
                return new AiChatSessionDto(
                        "1",
                        s.getTitle(),
                        s.getModelId() == null ? null : String.valueOf(s.getModelId()),
                        String.valueOf(s.getUserId()),
                        null);
            });

            AiChatSessionDto dto = service.createSession(request);

            assertThat(dto.modelId()).isEqualTo("42");
            ArgumentCaptor<AiChatSession> captor = ArgumentCaptor.forClass(AiChatSession.class);
            verify(sessionRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            verify(modelRepository, never()).findAll();
        }

        @Test
        @DisplayName("createSession 未指定 modelId 时自动选第一个启用模型")
        void createSession_auto_picks_first_enabled_model() {
            AiModel enabled = new AiModel();
            enabled.setId(55L);
            enabled.setEnabled(true);
            when(modelRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabled));
            AiChatSessionCreateRequest request = new AiChatSessionCreateRequest("自动", null);
            when(mapper.toSessionDto(any(AiChatSession.class))).thenAnswer(inv -> {
                AiChatSession s = inv.getArgument(0);
                return new AiChatSessionDto(
                        "1",
                        s.getTitle(),
                        s.getModelId() == null ? null : String.valueOf(s.getModelId()),
                        String.valueOf(s.getUserId()),
                        null);
            });

            AiChatSessionDto dto = service.createSession(request);

            assertThat(dto.modelId()).isEqualTo("55");
        }

        @Test
        @DisplayName("createSession modelId 为空白时走自动选择")
        void createSession_blank_modelId_auto_selects() {
            AiModel enabled = new AiModel();
            enabled.setId(9L);
            enabled.setEnabled(true);
            when(modelRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabled));
            AiChatSessionCreateRequest request = new AiChatSessionCreateRequest("空白", "  ");
            when(mapper.toSessionDto(any(AiChatSession.class))).thenAnswer(inv -> {
                AiChatSession s = inv.getArgument(0);
                return new AiChatSessionDto(
                        "1",
                        s.getTitle(),
                        s.getModelId() == null ? null : String.valueOf(s.getModelId()),
                        String.valueOf(s.getUserId()),
                        null);
            });

            AiChatSessionDto dto = service.createSession(request);

            assertThat(dto.modelId()).isEqualTo("9");
        }

        @Test
        @DisplayName("deleteSession 级联删除消息与会话")
        void deleteSession_removes_messages_then_session() {
            AiChatSession s = session(3L, 10L);
            when(sessionRepository.findByIdAndUserId(3L, USER_ID)).thenReturn(Optional.of(s));

            service.deleteSession(3L);

            verify(messageRepository).deleteBySessionId(3L);
            verify(sessionRepository).delete(s);
        }

        @Test
        @DisplayName("deleteSession 不存在抛 NOT_FOUND 且不删消息")
        void deleteSession_not_found() {
            when(sessionRepository.findByIdAndUserId(404L, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteSession(404L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
            verify(messageRepository, never()).deleteBySessionId(any());
            verify(sessionRepository, never()).delete(any());
        }

        @Test
        @DisplayName("未认证调用抛 UNAUTHORIZED")
        void unauthenticated_rejected() {
            SecurityContextHolder.clearContext();

            assertThatThrownBy(() -> service.listSessions())
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.UNAUTHORIZED);
        }
    }
}
