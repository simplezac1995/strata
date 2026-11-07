// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import dev.strata.ai.api.dto.AiBillingDailyPoint;
import dev.strata.ai.api.dto.AiBillingSummary;
import dev.strata.ai.api.dto.AiUsageAggregateItem;
import dev.strata.ai.biz.entity.AiLlmUsage;
import dev.strata.ai.biz.repository.AiLlmUsageRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiBillingAppService 用量聚合")
class AiBillingAppServiceTest {

    @Mock
    private AiLlmUsageRepository usageRepository;

    @InjectMocks
    private AiBillingAppService service;

    private static AiLlmUsage row(String model, String sessionId, int totalTokens, Instant createdAt) {
        AiLlmUsage r = new AiLlmUsage();
        r.setProvider("deepseek");
        r.setModel(model);
        r.setSessionId(sessionId);
        r.setPromptTokens(totalTokens / 2);
        r.setCompletionTokens(totalTokens / 2);
        r.setTotalTokens(totalTokens);
        r.setCostMinor(BigInteger.ZERO);
        r.setAction("office");
        r.setCreatedAt(createdAt);
        return r;
    }

    @Test
    @DisplayName("summary 按天聚合 totals 与 daily")
    void summary_groups_by_day_and_model() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS);
        String today = LocalDate.ofInstant(now, ZoneId.systemDefault()).toString();
        when(usageRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(
                        row("deepseek-chat", "s1", 100, now),
                        row("deepseek-chat", "s1", 50, now),
                        row("gpt-4o", "s2", 200, now)));

        AiBillingSummary summary = service.summary(30);

        assertThat(summary.days()).isEqualTo(30);
        assertThat(summary.daily()).hasSize(1);
        AiBillingDailyPoint day = summary.daily().getFirst();
        assertThat(day.date()).isEqualTo(today);
        assertThat(day.callCount()).isEqualTo(3);
        assertThat(day.totalTokens()).isEqualTo(350);
        assertThat(summary.totals().callCount()).isEqualTo(3);
        assertThat(summary.totals().totalTokens()).isEqualTo(350);
    }

    @Test
    @DisplayName("aggregate groupBy=model 按模型求和")
    void aggregate_by_model() {
        Instant now = Instant.now();
        when(usageRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(
                        row("deepseek-chat", "s1", 100, now),
                        row("deepseek-chat", "s1", 50, now),
                        row("gpt-4o", "s2", 200, now)));

        List<AiUsageAggregateItem> items = service.aggregate(30, "model");

        assertThat(items).hasSize(2);
        assertThat(items).extracting("groupKey").contains("deepseek-chat", "gpt-4o");
        assertThat(items).extracting("totalTokens").containsExactlyInAnyOrder(150L, 200L);
    }

    @Test
    @DisplayName("aggregate groupBy=user 按 sessionId 分组（无 user 列）")
    void aggregate_by_user_groups_by_session() {
        Instant now = Instant.now();
        when(usageRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of(row("m", "s1", 100, now), row("m", "s2", 250, now), row("m", null, 50, now)));

        List<AiUsageAggregateItem> items = service.aggregate(30, "user");

        assertThat(items).extracting("groupKey").contains("s1", "s2", "unknown");
        assertThat(items).extracting("totalTokens").containsExactlyInAnyOrder(100L, 250L, 50L);
    }

    @Test
    @DisplayName("空表返回空 daily/totals")
    void empty_ledger_returns_empty() {
        when(usageRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(any()))
                .thenReturn(List.of());

        AiBillingSummary summary = service.summary(30);
        assertThat(summary.daily()).isEmpty();
        assertThat(summary.totals().callCount()).isZero();
        assertThat(service.aggregate(30, "model")).isEmpty();
    }

    @Test
    @DisplayName("不支持的 groupBy 抛 BAD_REQUEST")
    void bad_group_by_rejected() {
        assertThatThrownBy(() -> service.aggregate(30, "hacker"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).errorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
