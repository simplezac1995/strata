// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.dto.Chat2SqlResponse;
import dev.strata.ai.api.request.Chat2SqlRequest;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.ai.config.biz.repository.AiModelRepository;
import dev.strata.ai.config.biz.repository.AiProviderRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmGateway;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiSqlAppService Chat2SQL")
class AiSqlAppServiceTest {

    @Mock
    private LlmGateway llmGateway;

    @Mock
    private AiProviderRepository providerRepository;

    @Mock
    private AiModelRepository modelRepository;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private AiUsageRecorder usageRecorder;

    private AiSqlAppService serviceWithRows(AiSqlAppService.QueryResult canned) {
        return new AiSqlAppService(
                llmGateway, providerRepository, modelRepository, dataSource, transactionManager, usageRecorder) {
            @Override
            QueryResult executeReadOnly(String sql) {
                return canned;
            }
        };
    }

    private void stubLlm() {
        AiProvider p = new AiProvider();
        p.setId(10L);
        p.setBaseUrl("https://llm");
        p.setApiKey("k");
        when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.of(p));
        AiModel m = new AiModel();
        m.setId(20L);
        m.setProviderId(10L);
        m.setCode("gpt");
        when(modelRepository.findFirstByEnabledTrueAndProviderIdOrderByIdAsc(10L))
                .thenReturn(Optional.of(m));
    }

    @Test
    void no_provider_rejected() {
        when(providerRepository.findFirstByEnabledTrueOrderByIdAsc()).thenReturn(Optional.empty());
        AiSqlAppService svc = new AiSqlAppService(
                llmGateway, providerRepository, modelRepository, dataSource, transactionManager, usageRecorder);

        assertThatThrownBy(() -> svc.chat2Sql(new Chat2SqlRequest("q", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void writes_are_rejected() {
        stubLlm();
        when(llmGateway.chatWithUsage(any(LlmChatCommand.class)))
                .thenReturn(new LlmChatResult("DROP TABLE sys_user;", 1, 1, 2, "gpt", "stop"));
        AiSqlAppService svc = serviceWithRows(new AiSqlAppService.QueryResult(List.of(), List.of()));

        assertThatThrownBy(() -> svc.chat2Sql(new Chat2SqlRequest("删表", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void select_runs_and_suggests_chart() {
        stubLlm();
        when(llmGateway.chatWithUsage(any(LlmChatCommand.class)))
                .thenReturn(new LlmChatResult(
                        "```sql\nSELECT status, count(*) FROM sys_user GROUP BY status;\n```", 1, 1, 2, "gpt", "stop"));
        AiSqlAppService.QueryResult canned = new AiSqlAppService.QueryResult(
                List.of("status", "count"), List.of(java.util.Map.of("status", "active", "count", 5)));
        AiSqlAppService svc = serviceWithRows(canned);

        Chat2SqlResponse resp = svc.chat2Sql(new Chat2SqlRequest("按状态统计用户", null));

        assertThat(resp.sql()).isEqualTo("SELECT status, count(*) FROM sys_user GROUP BY status");
        assertThat(resp.columns()).containsExactly("status", "count");
        assertThat(resp.rows()).hasSize(1);
        assertThat(resp.chartSuggestion()).isEqualTo("bar");
        verify(llmGateway).chatWithUsage(any());
    }

    @Test
    void non_select_reply_rejected() {
        stubLlm();
        when(llmGateway.chatWithUsage(any(LlmChatCommand.class)))
                .thenReturn(new LlmChatResult("Sorry I can't do that.", 1, 1, 2, "gpt", "stop"));
        AiSqlAppService svc = serviceWithRows(new AiSqlAppService.QueryResult(List.of(), List.of()));

        assertThatThrownBy(() -> svc.chat2Sql(new Chat2SqlRequest("q", null)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
