// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.service;

import dev.strata.ai.api.dto.AiBillingDailyPoint;
import dev.strata.ai.api.dto.AiBillingSummary;
import dev.strata.ai.api.dto.AiUsageAggregateItem;
import dev.strata.ai.biz.entity.AiLlmUsage;
import dev.strata.ai.biz.repository.AiLlmUsageRepository;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Billing / usage aggregation over {@code ai_llm_usage}. Returns the shape the admin UI expects:
 * {@code { days, daily[], totals }} for the summary endpoint and rows with {@code groupKey, callCount,
 * prompt/completion/totalTokens, costMinor} for the aggregate endpoint.
 */
@Service
public class AiBillingAppService {

    private static final Set<String> GROUP_BY = Set.of("model", "user", "day");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AiLlmUsageRepository usageRepository;

    public AiBillingAppService(AiLlmUsageRepository usageRepository) {
        this.usageRepository = usageRepository;
    }

    @Transactional(readOnly = true)
    public AiBillingSummary summary(int days) {
        List<AiLlmUsage> rows = loadWindow(days);
        Map<LocalDate, long[]> byDay = new LinkedHashMap<>();
        long[] total = new long[5]; // callCount, prompt, completion, total, costMinor
        for (AiLlmUsage row : rows) {
            LocalDate day = LocalDate.ofInstant(row.getCreatedAt(), ZoneId.systemDefault());
            long[] bucket = byDay.computeIfAbsent(day, d -> new long[4]); // callCount, prompt, completion, total
            bucket[0] += 1;
            bucket[1] += nz(row.getPromptTokens());
            bucket[2] += nz(row.getCompletionTokens());
            bucket[3] += nz(row.getTotalTokens());
            total[0] += 1;
            total[1] += nz(row.getPromptTokens());
            total[2] += nz(row.getCompletionTokens());
            total[3] += nz(row.getTotalTokens());
            total[4] += row.getCostMinor() == null ? 0 : row.getCostMinor().longValue();
        }

        List<AiBillingDailyPoint> daily = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> e : byDay.entrySet()) {
            long[] b = e.getValue();
            daily.add(new AiBillingDailyPoint(e.getKey().format(DAY), b[0], b[1], b[2], b[3], BigDecimal.ZERO));
        }

        return new AiBillingSummary(
                days,
                daily,
                new AiBillingSummary.Totals(
                        total[0],
                        total[1],
                        total[2],
                        total[3],
                        BigDecimal.valueOf(total[4], 2).setScale(2, RoundingMode.HALF_UP)));
    }

    @Transactional(readOnly = true)
    public List<AiUsageAggregateItem> aggregate(int days, String groupBy) {
        String key = groupBy == null ? "model" : groupBy.toLowerCase(Locale.ROOT);
        if (!GROUP_BY.contains(key)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Unsupported groupBy: " + groupBy);
        }
        List<AiLlmUsage> rows = loadWindow(days);
        Map<String, long[]> grouped = new LinkedHashMap<>();
        for (AiLlmUsage row : rows) {
            String bucket =
                    switch (key) {
                        case "model" -> row.getModel();
                        case "user" ->
                            row.getSessionId() == null || row.getSessionId().isBlank() ? "unknown" : row.getSessionId();
                        default ->
                            LocalDate.ofInstant(row.getCreatedAt(), ZoneId.systemDefault())
                                    .format(DAY);
                    };
            long[] acc = grouped.computeIfAbsent(bucket, k -> new long[5]);
            acc[0] += 1;
            acc[1] += nz(row.getPromptTokens());
            acc[2] += nz(row.getCompletionTokens());
            acc[3] += nz(row.getTotalTokens());
            acc[4] += row.getCostMinor() == null ? 0 : row.getCostMinor().longValue();
        }
        List<AiUsageAggregateItem> result = new ArrayList<>();
        for (Map.Entry<String, long[]> e : grouped.entrySet()) {
            long[] a = e.getValue();
            result.add(new AiUsageAggregateItem(
                    e.getKey(),
                    a[0],
                    a[1],
                    a[2],
                    a[3],
                    BigDecimal.valueOf(a[4], 2).setScale(2, RoundingMode.HALF_UP)));
        }
        return result;
    }

    private List<AiLlmUsage> loadWindow(int days) {
        int window = Math.max(1, days);
        Instant since = Instant.now().minusSeconds((long) window * 24L * 60L * 60L);
        return usageRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(since);
    }

    private static long nz(Integer v) {
        return v == null ? 0L : v.longValue();
    }
}
