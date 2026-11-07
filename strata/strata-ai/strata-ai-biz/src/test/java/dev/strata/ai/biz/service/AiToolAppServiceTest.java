// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.dto.AiToolRunDto;
import dev.strata.ai.api.dto.AiUsageDto;
import dev.strata.ai.api.request.AiToolRunRequest;
import dev.strata.ai.chat.biz.repository.AiChatMessageRepository;
import dev.strata.ai.chat.biz.repository.AiChatSessionRepository;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.ai.knowledge.biz.repository.AiDocumentRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmGateway;
import dev.strata.system.api.support.SensitiveWordScanner;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiToolAppService AI 工具调用")
class AiToolAppServiceTest {

    @Mock
    private LlmGateway llmGateway;

    @Mock
    private AiProviderRepository providerRepository;

    @Mock
    private AiModelRepository modelRepository;

    @Mock
    private SensitiveWordScanner scanner;

    @Mock
    private AiChatSessionRepository sessionRepository;

    @Mock
    private AiChatMessageRepository messageRepository;

    @Mock
    private AiDocumentRepository documentRepository;

    @InjectMocks
    private AiToolAppService service;

    private AiProvider enabledProvider(long id) {
        AiProvider p = new AiProvider();
        p.setId(id);
        p.setEnabled(true);
        p.setBaseUrl("https://llm");
        p.setApiKey("k");
        return p;
    }

    private AiModel enabledModel(long id, Long providerId) {
        AiModel m = new AiModel();
        m.setId(id);
        m.setProviderId(providerId);
        m.setCode("gpt");
        m.setEnabled(true);
        return m;
    }

    @Nested
    @DisplayName("usage 统计")
    class Usage {

        @Test
        @DisplayName("usage 汇总各表行数")
        void usage_sums_counts() {
            when(sessionRepository.count()).thenReturn(3L);
            when(messageRepository.count()).thenReturn(40L);
            when(providerRepository.count()).thenReturn(2L);
            when(modelRepository.count()).thenReturn(5L);
            when(documentRepository.count()).thenReturn(7L);

            AiUsageDto dto = service.usage();

            assertThat(dto.sessionCount()).isEqualTo(3);
            assertThat(dto.messageCount()).isEqualTo(40);
            assertThat(dto.providerCount()).isEqualTo(2);
            assertThat(dto.modelCount()).isEqualTo(5);
            assertThat(dto.documentCount()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("run 工具分发")
    class Run {

        @Test
        @DisplayName("文本工具：本地无敏感词时直接返回 LLM 结果")
        void text_tool_returns_llm_result() {
            when(scanner.scanHits("正文")).thenReturn("");
            when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledProvider(10L)));
            when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                    .thenReturn(Optional.of(enabledModel(20L, 10L)));
            when(llmGateway.chat(any(LlmChatCommand.class))).thenReturn("润色后文本");

            AiToolRunDto dto = service.run(new AiToolRunRequest("proofread", "正文"));

            assertThat(dto.tool()).isEqualTo("proofread");
            assertThat(dto.result()).isEqualTo("润色后文本");
            assertThat(dto.usedLlm()).isTrue();
        }

        @Test
        @DisplayName("文本工具：工具名先 trim 再转小写")
        void tool_name_is_trimmed_and_lowercased() {
            when(scanner.scanHits("c")).thenReturn("");
            when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledProvider(10L)));
            when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                    .thenReturn(Optional.of(enabledModel(20L, 10L)));
            when(llmGateway.chat(any(LlmChatCommand.class))).thenReturn("out");

            AiToolRunDto dto = service.run(new AiToolRunRequest("  RewRite  ", "c"));

            assertThat(dto.tool()).isEqualTo("rewrite");
        }

        @Test
        @DisplayName("本地命中敏感词时结果拼接本地命中与模型复审")
        void local_hits_are_prepended() {
            when(scanner.scanHits("含敏感词")).thenReturn("违规词X");
            when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledProvider(10L)));
            when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                    .thenReturn(Optional.of(enabledModel(20L, 10L)));
            when(llmGateway.chat(any(LlmChatCommand.class))).thenReturn("模型意见");

            AiToolRunDto dto = service.run(new AiToolRunRequest("safety", "含敏感词"));

            assertThat(dto.result()).startsWith("## Local sensitive-word hits\n违规词X");
            assertThat(dto.result()).contains("## Model review\n模型意见");
            assertThat(dto.usedLlm()).isTrue();
        }

        @Test
        @DisplayName("prompt 组装工具指令与原文")
        void prompt_includes_instruction_and_content() {
            when(scanner.scanHits("CODE")).thenReturn("");
            when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledProvider(10L)));
            when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                    .thenReturn(Optional.of(enabledModel(20L, 10L)));
            when(llmGateway.chat(any(LlmChatCommand.class))).thenReturn("ok");

            service.run(new AiToolRunRequest("code-review", "CODE"));

            ArgumentCaptor<LlmChatCommand> captor = ArgumentCaptor.forClass(LlmChatCommand.class);
            verify(llmGateway).chat(captor.capture());
            assertThat(captor.getValue().userMessage())
                    .contains("Review the following code or diff")
                    .endsWith("CODE");
        }

        @Test
        @DisplayName("模型未绑定到当前供应商时回退到任一启用模型")
        void fallback_to_any_enabled_model() {
            when(scanner.scanHits("c")).thenReturn("");
            when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledProvider(10L)));
            when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                    .thenReturn(Optional.empty());
            when(modelRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledModel(20L, 99L)));
            when(llmGateway.chat(any(LlmChatCommand.class))).thenReturn("out");

            AiToolRunDto dto = service.run(new AiToolRunRequest("outline", "c"));

            assertThat(dto.usedLlm()).isTrue();
        }

        @Test
        @DisplayName("LLM 失败且本地命中敏感词时降级返回本地结果并标记未用模型")
        void llm_failure_falls_back_to_local_hits() {
            when(scanner.scanHits("敏感")).thenReturn("敏感词Y");
            when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledProvider(10L)));
            when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                    .thenReturn(Optional.of(enabledModel(20L, 10L)));
            when(llmGateway.chat(any(LlmChatCommand.class)))
                    .thenThrow(new BusinessException(ErrorCode.UPSTREAM_UNAVAILABLE, "down"));

            AiToolRunDto dto = service.run(new AiToolRunRequest("safety", "敏感"));

            assertThat(dto.result()).startsWith("## Local sensitive-word hits\n敏感词Y");
            assertThat(dto.result()).contains("(LLM unavailable: down)");
            assertThat(dto.usedLlm()).isFalse();
        }

        @Test
        @DisplayName("LLM 失败且本地无命中时异常上抛")
        void llm_failure_without_local_hits_rethrows() {
            when(scanner.scanHits("c")).thenReturn("");
            when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledProvider(10L)));
            when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                    .thenReturn(Optional.of(enabledModel(20L, 10L)));
            when(llmGateway.chat(any(LlmChatCommand.class)))
                    .thenThrow(new BusinessException(ErrorCode.UPSTREAM_UNAVAILABLE, "down"));

            assertThatThrownBy(() -> service.run(new AiToolRunRequest("proofread", "c")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.UPSTREAM_UNAVAILABLE);
        }

        @Test
        @DisplayName("无启用供应商时抛 BAD_REQUEST")
        void no_provider_rejected() {
            when(scanner.scanHits("c")).thenReturn("");
            when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.run(new AiToolRunRequest("proofread", "c")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST);
            verify(llmGateway, never()).chat(any());
        }

        @Test
        @DisplayName("无启用模型时抛 BAD_REQUEST")
        void no_model_rejected() {
            when(scanner.scanHits("c")).thenReturn("");
            when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledProvider(10L)));
            when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                    .thenReturn(Optional.empty());
            when(modelRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.run(new AiToolRunRequest("proofread", "c")))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST);
            verify(llmGateway, never()).chat(any());
        }

        @Test
        @DisplayName("未配置的媒体工具抛 BAD_REQUEST")
        void media_tool_not_configured() {
            assertThatThrownBy(() -> service.run(new AiToolRunRequest("poster", "x")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Poster tool not configured")
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST);
            verify(llmGateway, never()).chat(any());
        }

        @Test
        @DisplayName("TTS 元数据解析 voice 与 speed")
        void tts_meta_parses_voice_and_speed() {
            AiToolAppService.TtsParams params =
                    AiToolAppService.parseTtsMeta("Welcome to Strata Admin.\n[voice=longwan, speed=1.5]");

            assertThat(params.text()).isEqualTo("Welcome to Strata Admin.");
            assertThat(params.voice()).isEqualTo("longwan");
            assertThat(params.speed()).isEqualTo(1.5f);
        }

        @Test
        @DisplayName("TTS 无元数据时使用默认音色与语速")
        void tts_meta_defaults() {
            AiToolAppService.TtsParams params = AiToolAppService.parseTtsMeta("hello");

            assertThat(params.text()).isEqualTo("hello");
            assertThat(params.voice()).isEqualTo("longxiaochun");
            assertThat(params.speed()).isEqualTo(1.0f);
        }

        @Test
        @DisplayName("TTS 语速越界时收敛到 0.5~2.0")
        void tts_speed_is_clamped() {
            AiToolAppService.TtsParams params = AiToolAppService.parseTtsMeta("x[voice=longcheng, speed=9]");

            assertThat(params.voice()).isEqualTo("longcheng");
            assertThat(params.speed()).isEqualTo(2.0f);
        }

        @Test
        @DisplayName("前端 v1 音色映射到 cosyvoice-v3 音色")
        void tts_voice_maps_to_v3() {
            assertThat(AiToolAppService.toV3Voice("longxiaochun")).isEqualTo("longxiaochun_v3");
            assertThat(AiToolAppService.toV3Voice("longwan")).isEqualTo("longwan_v3");
            assertThat(AiToolAppService.toV3Voice("longcheng")).isEqualTo("longcheng_v3");
            assertThat(AiToolAppService.toV3Voice("longjielidou")).isEqualTo("longjielidou_v3");
        }

        @Test
        @DisplayName("未知音色原样透传")
        void tts_unknown_voice_passes_through() {
            assertThat(AiToolAppService.toV3Voice("custom_voice")).isEqualTo("custom_voice");
        }

        @Test
        @DisplayName("未知工具抛 BAD_REQUEST")
        void unknown_tool_rejected() {
            assertThatThrownBy(() -> service.run(new AiToolRunRequest("teleport", "x")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Unknown AI tool: teleport")
                    .extracting(ex -> ((BusinessException) ex).errorCode())
                    .isEqualTo(ErrorCode.BAD_REQUEST);
            verify(llmGateway, never()).chat(any());
        }
    }
}
