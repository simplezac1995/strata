// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.request.AiChatAttachment;
import dev.strata.ai.api.request.AiChatMessageCreateRequest;
import dev.strata.ai.chat.api.dto.AiChatMessageDto;
import dev.strata.ai.chat.api.entity.AiChatMessage;
import dev.strata.ai.chat.api.entity.AiChatSession;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatCommand;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatAppService 消息流")
class AiChatAppServiceMessageTest extends AiChatAppServiceTestBase {

    @Nested
    @DisplayName("消息 list / send")
    class MessageFlow {

        private AiProvider provider() {
            AiProvider p = new AiProvider();
            p.setBaseUrl("https://llm");
            p.setApiKey("sk");
            return p;
        }

        private AiModel model(long providerId) {
            AiModel m = new AiModel();
            m.setProviderId(providerId);
            m.setCode("gpt-test");
            return m;
        }

        @Test
        @DisplayName("listMessages 校验归属后按时间升序映射")
        void listMessages_returns_history() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            AiChatMessage m1 = new AiChatMessage();
            m1.setRole("user");
            when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of(m1));
            when(mapper.toMessageDto(m1)).thenReturn(new AiChatMessageDto("1", "1", "user", "hi", null));

            List<AiChatMessageDto> result = service.listMessages(1L);

            assertThat(result).hasSize(1);
            assertThat(result.getFirst().role()).isEqualTo("user");
        }

        @Test
        @DisplayName("sendMessage 正常路径：发历史给 LLM，落库 user+assistant 两条并返回回复")
        void sendMessage_happy_path() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(10L)).thenReturn(Optional.of(model(20L)));
            when(providerRepository.findById(20L)).thenReturn(Optional.of(provider()));
            AiChatMessage past = new AiChatMessage();
            past.setRole("user");
            past.setContent("earlier");
            when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of(past));
            when(llmGateway.chatWithUsage(any(LlmChatCommand.class))).thenReturn(aiResult("好的！"));
            when(mapper.toMessageDto(any(AiChatMessage.class))).thenAnswer(inv -> {
                AiChatMessage m = inv.getArgument(0);
                return new AiChatMessageDto("9", "1", m.getRole(), m.getContent(), null);
            });

            AiChatMessageDto dto = service.sendMessage(1L, new AiChatMessageCreateRequest("你好", null));

            assertThat(dto.content()).isEqualTo("好的！");
            assertThat(dto.role()).isEqualTo("assistant");

            ArgumentCaptor<LlmChatCommand> cmdCaptor = ArgumentCaptor.forClass(LlmChatCommand.class);
            verify(llmGateway).chatWithUsage(cmdCaptor.capture());
            LlmChatCommand cmd = cmdCaptor.getValue();
            assertThat(cmd.userMessage()).isEqualTo("你好");
            assertThat(cmd.baseUrl()).isEqualTo("https://llm");
            assertThat(cmd.modelCode()).isEqualTo("gpt-test");
            // 历史在新消息落库之前读取，含 system 提示 + 上一轮
            assertThat(cmd.history()).hasSize(2);
            assertThat(cmd.history().get(0).role()).isEqualTo("system");
            assertThat(cmd.history().get(1).content()).isEqualTo("earlier");

            ArgumentCaptor<AiChatMessage> msgCaptor = ArgumentCaptor.forClass(AiChatMessage.class);
            verify(messageRepository, org.mockito.Mockito.times(2)).save(msgCaptor.capture());
            assertThat(msgCaptor.getAllValues().get(0).getRole()).isEqualTo("user");
            assertThat(msgCaptor.getAllValues().get(0).getContent()).isEqualTo("你好");
            assertThat(msgCaptor.getAllValues().get(1).getRole()).isEqualTo("assistant");
        }

        @Test
        @DisplayName("sendMessage LLM 抛异常时仍落库 user 消息与 assistant 错误占位")
        void sendMessage_llm_failure_persists_user_message_and_error_placeholder() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(10L)).thenReturn(Optional.of(model(20L)));
            when(providerRepository.findById(20L)).thenReturn(Optional.of(provider()));
            when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());
            when(llmGateway.chatWithUsage(any(LlmChatCommand.class))).thenThrow(new RuntimeException("upstream 503"));
            when(mapper.toMessageDto(any(AiChatMessage.class))).thenAnswer(inv -> {
                AiChatMessage m = inv.getArgument(0);
                return new AiChatMessageDto("9", "1", m.getRole(), m.getContent(), null);
            });

            AiChatMessageDto dto = service.sendMessage(1L, new AiChatMessageCreateRequest("你好", null));

            // The error placeholder is returned to the caller, not the raw exception.
            assertThat(dto.role()).isEqualTo("assistant");
            assertThat(dto.content()).isEqualTo("LLM unavailable, please retry");

            ArgumentCaptor<AiChatMessage> msgCaptor = ArgumentCaptor.forClass(AiChatMessage.class);
            verify(messageRepository, org.mockito.Mockito.times(2)).save(msgCaptor.capture());
            // The user's prompt survives the outage.
            assertThat(msgCaptor.getAllValues().get(0).getRole()).isEqualTo("user");
            assertThat(msgCaptor.getAllValues().get(0).getContent()).isEqualTo("你好");
            // Followed by the assistant error placeholder.
            assertThat(msgCaptor.getAllValues().get(1).getRole()).isEqualTo("assistant");
            assertThat(msgCaptor.getAllValues().get(1).getContent()).isEqualTo("LLM unavailable, please retry");
        }

        @Test
        @DisplayName("历史窗口限量下推到仓储（只取最新 N 条喂给 LLM）")
        void sendMessage_boundsHistoryWindowViaPageable() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(10L)).thenReturn(Optional.of(model(20L)));
            when(providerRepository.findById(20L)).thenReturn(Optional.of(provider()));
            when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());
            when(llmGateway.chatWithUsage(any(LlmChatCommand.class))).thenReturn(aiResult("ok"));
            when(mapper.toMessageDto(any(AiChatMessage.class)))
                    .thenReturn(new AiChatMessageDto("9", "1", "assistant", "ok", null));

            service.sendMessage(1L, new AiChatMessageCreateRequest("hi", null));

            ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(messageRepository).findBySessionIdOrderByCreatedAtDesc(eq(1L), pageCaptor.capture());
            assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(30);
            assertThat(pageCaptor.getValue().getPageNumber()).isZero();
        }

        @Test
        @DisplayName("sendMessage 会话未配置模型抛 BAD_REQUEST")
        void sendMessage_no_model_rejected() {
            AiChatSession s = session(1L, null);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));

            assertThatThrownBy(() -> service.sendMessage(1L, new AiChatMessageCreateRequest("hi", null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST);
            verify(llmGateway, never()).chatWithUsage(any());
        }

        @Test
        @DisplayName("sendMessage 模型不存在抛 BAD_REQUEST")
        void sendMessage_model_missing() {
            AiChatSession s = session(1L, 99L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.sendMessage(1L, new AiChatMessageCreateRequest("hi", null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST);
            verify(llmGateway, never()).chatWithUsage(any());
        }

        @Test
        @DisplayName("sendMessage 供应商不存在抛 BAD_REQUEST")
        void sendMessage_provider_missing() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(10L)).thenReturn(Optional.of(model(20L)));
            when(providerRepository.findById(20L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.sendMessage(1L, new AiChatMessageCreateRequest("hi", null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST);
            verify(llmGateway, never()).chatWithUsage(any());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("sendMessage 会话不存在抛 NOT_FOUND")
        void sendMessage_session_not_found() {
            when(sessionRepository.findByIdAndUserId(404L, USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.sendMessage(404L, new AiChatMessageCreateRequest("hi", null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.NOT_FOUND);
        }

        @Test
        @DisplayName("sendMessage 携带图片附件时把多模态段透传给 LLM 网关")
        void sendMessage_with_image_attachment_parts_forwarded() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(10L)).thenReturn(Optional.of(model(20L)));
            when(providerRepository.findById(20L)).thenReturn(Optional.of(provider()));
            when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());
            when(llmGateway.chatWithUsage(any(LlmChatCommand.class))).thenReturn(aiResult("看图回答"));
            when(mapper.toMessageDto(any(AiChatMessage.class))).thenAnswer(inv -> {
                AiChatMessage m = inv.getArgument(0);
                return new AiChatMessageDto("9", "1", m.getRole(), m.getContent(), null);
            });
            AiChatAttachment image = new AiChatAttachment("ai-chat", "pics/x.png", "image/png", "x.png", 12L);

            service.sendMessage(1L, new AiChatMessageCreateRequest("描述它", List.of(image)));

            ArgumentCaptor<LlmChatCommand> cmdCaptor = ArgumentCaptor.forClass(LlmChatCommand.class);
            verify(llmGateway).chatWithUsage(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().userParts()).hasSize(2);
            assertThat(cmdCaptor.getValue().userParts().get(1).type()).isEqualTo("image_url");
            assertThat(cmdCaptor.getValue().userParts().get(1).imageUrl())
                    .isEqualTo("/api/v1/system/files/ai-chat/pics/x.png/download");
        }

        @Test
        @DisplayName("sendMessage 配置了公网基址时 image_url 被前缀为绝对地址")
        void sendMessage_public_base_url_prefixes_absolute_image_url() {
            AiChatAppService publicBaseService = serviceWithPublicBase("https://admin.example.com");
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(10L)).thenReturn(Optional.of(model(20L)));
            when(providerRepository.findById(20L)).thenReturn(Optional.of(provider()));
            when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());
            when(llmGateway.chatWithUsage(any(LlmChatCommand.class))).thenReturn(aiResult("看图"));
            when(mapper.toMessageDto(any(AiChatMessage.class))).thenAnswer(inv -> {
                AiChatMessage m = inv.getArgument(0);
                return new AiChatMessageDto("9", "1", m.getRole(), m.getContent(), null);
            });
            AiChatAttachment image = new AiChatAttachment("ai-chat", "pics/x.png", "image/png", "x.png", 12L);

            publicBaseService.sendMessage(1L, new AiChatMessageCreateRequest("描述它", List.of(image)));

            ArgumentCaptor<LlmChatCommand> cmdCaptor = ArgumentCaptor.forClass(LlmChatCommand.class);
            verify(llmGateway).chatWithUsage(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().userParts().get(1).imageUrl())
                    .isEqualTo("https://admin.example.com/api/v1/system/files/ai-chat/pics/x.png/download");
        }

        @Test
        @DisplayName("sendMessage 1 图 1 文件：网关命令含 image_url 段且文件被文本化")
        void sendMessage_image_and_file_mixed_parts_forwarded() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(10L)).thenReturn(Optional.of(model(20L)));
            when(providerRepository.findById(20L)).thenReturn(Optional.of(provider()));
            when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());
            when(llmGateway.chatWithUsage(any(LlmChatCommand.class))).thenReturn(aiResult("回答"));
            when(mapper.toMessageDto(any(AiChatMessage.class))).thenAnswer(inv -> {
                AiChatMessage m = inv.getArgument(0);
                return new AiChatMessageDto("9", "1", m.getRole(), m.getContent(), null);
            });
            AiChatAttachment image = new AiChatAttachment("ai-chat", "pics/x.png", "image/png", "x.png", 12L);
            AiChatAttachment pdf = new AiChatAttachment("ai-chat", "docs/b.pdf", "application/pdf", "b.pdf", 2048L);

            service.sendMessage(1L, new AiChatMessageCreateRequest("看下这俩", List.of(image, pdf)));

            ArgumentCaptor<LlmChatCommand> cmdCaptor = ArgumentCaptor.forClass(LlmChatCommand.class);
            verify(llmGateway).chatWithUsage(cmdCaptor.capture());
            LlmChatCommand cmd = cmdCaptor.getValue();
            assertThat(cmd.userParts()).hasSize(2);
            assertThat(cmd.userParts().get(0).type()).isEqualTo("text");
            assertThat(cmd.userParts().get(0).text()).isEqualTo("看下这俩\n[已上传文件] b.pdf (2048 字节)");
            assertThat(cmd.userParts().get(1).type()).isEqualTo("image_url");
            assertThat(cmd.userParts().get(1).imageUrl()).isEqualTo("/api/v1/system/files/ai-chat/pics/x.png/download");
            // 文件说明同样落在纯文本 userMessage 上，保证无图片模型也能看到文件名
            assertThat(cmd.userMessage()).contains("[已上传文件] b.pdf (2048 字节)");
        }

        @Test
        @DisplayName("sendMessage 纯文本无附件时 userParts 为 null（向后兼容）")
        void sendMessage_plain_text_has_null_parts() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(10L)).thenReturn(Optional.of(model(20L)));
            when(providerRepository.findById(20L)).thenReturn(Optional.of(provider()));
            when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());
            when(llmGateway.chatWithUsage(any(LlmChatCommand.class))).thenReturn(aiResult("ok"));
            when(mapper.toMessageDto(any(AiChatMessage.class))).thenAnswer(inv -> {
                AiChatMessage m = inv.getArgument(0);
                return new AiChatMessageDto("9", "1", m.getRole(), m.getContent(), null);
            });

            service.sendMessage(1L, new AiChatMessageCreateRequest("hi", null));

            ArgumentCaptor<LlmChatCommand> cmdCaptor = ArgumentCaptor.forClass(LlmChatCommand.class);
            verify(llmGateway).chatWithUsage(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().userParts()).isNull();
            assertThat(cmdCaptor.getValue().userMessage()).isEqualTo("hi");
        }

        @Test
        @DisplayName("sendMessage 内容与附件均为空时抛 BAD_REQUEST 且不调用 LLM")
        void sendMessage_empty_content_and_attachments_rejected() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(10L)).thenReturn(Optional.of(model(20L)));
            when(providerRepository.findById(20L)).thenReturn(Optional.of(provider()));

            assertThatThrownBy(() -> service.sendMessage(1L, new AiChatMessageCreateRequest("   ", List.of())))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST);
            verify(llmGateway, never()).chatWithUsage(any());
            verify(messageRepository, never()).save(any());
        }

        @Test
        @DisplayName("sendMessage 仅图片无文本时落库内容为附件摘要")
        void sendMessage_image_only_persists_attachment_summary() {
            AiChatSession s = session(1L, 10L);
            when(sessionRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(s));
            when(modelRepository.findById(10L)).thenReturn(Optional.of(model(20L)));
            when(providerRepository.findById(20L)).thenReturn(Optional.of(provider()));
            when(messageRepository.findBySessionIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
                    .thenReturn(List.of());
            when(llmGateway.chatWithUsage(any(LlmChatCommand.class))).thenReturn(aiResult("回复"));
            when(mapper.toMessageDto(any(AiChatMessage.class))).thenAnswer(inv -> {
                AiChatMessage m = inv.getArgument(0);
                return new AiChatMessageDto("9", "1", m.getRole(), m.getContent(), null);
            });
            AiChatAttachment image = new AiChatAttachment("ai-chat", "only.png", "image/png", "only.png", 3L);

            service.sendMessage(1L, new AiChatMessageCreateRequest("", List.of(image)));

            ArgumentCaptor<AiChatMessage> msgCaptor = ArgumentCaptor.forClass(AiChatMessage.class);
            verify(messageRepository, org.mockito.Mockito.times(2)).save(msgCaptor.capture());
            AiChatMessage userMsg = msgCaptor.getAllValues().get(0);
            assertThat(userMsg.getRole()).isEqualTo("user");
            assertThat(userMsg.getContent()).contains("only.png");
        }
    }
}
