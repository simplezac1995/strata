// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import dev.strata.ai.biz.entity.AiLlmUsage;
import dev.strata.ai.biz.repository.AiLlmUsageRepository;
import dev.strata.plugin.api.llm.LlmChatResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiUsageRecorder 用量落库")
class AiUsageRecorderTest {

    @Mock
    private AiLlmUsageRepository usageRepository;

    @InjectMocks
    private AiUsageRecorder recorder;

    @Test
    @DisplayName("record 把 result 的 token 计数映射到实体并保存")
    void record_maps_fields_and_saves() {
        LlmChatResult result = new LlmChatResult("hi", 11, 9, 20, "deepseek-chat", "stop");

        recorder.record("deepseek", "deepseek-chat", "sess-1", "office", result);

        ArgumentCaptor<AiLlmUsage> captor = ArgumentCaptor.forClass(AiLlmUsage.class);
        verify(usageRepository).save(captor.capture());
        AiLlmUsage row = captor.getValue();
        assertThat(row.getProvider()).isEqualTo("deepseek");
        assertThat(row.getModel()).isEqualTo("deepseek-chat");
        assertThat(row.getSessionId()).isEqualTo("sess-1");
        assertThat(row.getAction()).isEqualTo("office");
        assertThat(row.getPromptTokens()).isEqualTo(11);
        assertThat(row.getCompletionTokens()).isEqualTo(9);
        assertThat(row.getTotalTokens()).isEqualTo(20);
        assertThat(row.getCostMinor()).isZero();
    }

    @Test
    @DisplayName("result 缺 token 时按 0 落库，provider/action 为空时有兜底")
    void null_tokens_default_to_zero_and_blanks_fallback() {
        LlmChatResult result = new LlmChatResult("hi", null, null, null, null, null);

        recorder.record(null, null, null, null, result);

        ArgumentCaptor<AiLlmUsage> captor = ArgumentCaptor.forClass(AiLlmUsage.class);
        verify(usageRepository).save(captor.capture());
        AiLlmUsage row = captor.getValue();
        assertThat(row.getProvider()).isEqualTo("unknown");
        assertThat(row.getModel()).isEqualTo("unknown");
        assertThat(row.getAction()).isEqualTo("chat");
        assertThat(row.getTotalTokens()).isZero();
    }

    @Test
    @DisplayName("落库异常被吞掉，不影响主调用")
    void repository_failure_is_swallowed() {
        doThrow(new RuntimeException("db down")).when(usageRepository).save(any(AiLlmUsage.class));
        LlmChatResult result = new LlmChatResult("hi", 1, 1, 2, "m", "stop");

        assertThatCode(() -> recorder.record("p", "m", null, "office", result)).doesNotThrowAnyException();
    }
}
