// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.dto.AiOfficeResponse;
import dev.strata.ai.api.request.AiOfficeRequest;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmGateway;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiOfficeAppServiceTest {

    @Mock
    private LlmGateway llmGateway;

    @Mock
    private AiProviderRepository providerRepository;

    @Mock
    private AiModelRepository modelRepository;

    @Mock
    private AiUsageRecorder usageRecorder;

    @InjectMocks
    private AiOfficeAppService service;

    private AiProvider enabledProvider() {
        AiProvider p = new AiProvider();
        p.setId(10L);
        p.setCode("deepseek");
        p.setEnabled(true);
        p.setBaseUrl("https://api.deepseek.com/v1");
        p.setApiKey("k");
        return p;
    }

    private AiModel enabledModel() {
        AiModel m = new AiModel();
        m.setId(20L);
        m.setProviderId(10L);
        m.setCode("deepseek-chat");
        m.setEnabled(true);
        return m;
    }

    @Test
    void generate_calls_llm_and_records_usage() {
        when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledProvider()));
        when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                .thenReturn(Optional.of(enabledModel()));
        when(llmGateway.chatWithUsage(any(LlmChatCommand.class)))
                .thenReturn(new LlmChatResult("营销文案", 12, 30, 42, "deepseek-chat", "stop"));

        AiOfficeResponse resp = service.run("generate", new AiOfficeRequest("新品耳机", null));

        assertThat(resp.result()).isEqualTo("营销文案");
        assertThat(resp.usage().promptTokens()).isEqualTo(12);
        assertThat(resp.usage().completionTokens()).isEqualTo(30);
        assertThat(resp.usage().totalTokens()).isEqualTo(42);

        ArgumentCaptor<LlmChatCommand> cmd = ArgumentCaptor.forClass(LlmChatCommand.class);
        verify(llmGateway).chatWithUsage(cmd.capture());
        assertThat(cmd.getValue().userMessage())
                .contains("professional copywriter")
                .endsWith("新品耳机");

        verify(usageRecorder).record(eq("deepseek"), eq("deepseek-chat"), eq(null), eq("office"), any());
    }

    @Test
    void translate_uses_translate_prompt() {
        when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(enabledProvider()));
        when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                .thenReturn(Optional.of(enabledModel()));
        when(llmGateway.chatWithUsage(any(LlmChatCommand.class)))
                .thenReturn(new LlmChatResult("out", 1, 1, 2, "m", "stop"));

        service.run("translate", new AiOfficeRequest("你好", null));

        ArgumentCaptor<LlmChatCommand> cmd = ArgumentCaptor.forClass(LlmChatCommand.class);
        verify(llmGateway).chatWithUsage(cmd.capture());
        assertThat(cmd.getValue().userMessage()).contains("Translate the following text");
    }

    @Test
    void unsupported_action_rejected() {
        assertThatThrownBy(() -> service.run("teleport", new AiOfficeRequest("x", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(llmGateway, never()).chatWithUsage(any());
        verify(usageRecorder, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void no_provider_rejected() {
        when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.run("generate", new AiOfficeRequest("x", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
        verify(llmGateway, never()).chatWithUsage(any());
    }
}
