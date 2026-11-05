// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.service;

import dev.strata.system.api.dto.ApiStatsDto;
import dev.strata.system.api.dto.ApiStatsDto.PathStat;
import dev.strata.system.ops.biz.repository.ApiCallStatRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * API call statistics, aggregated from the {@code api_call_stat} log written by {@code ApiStatsFilter}. The query
 * reads a rolling {@code days} window and projects totals plus a per-path breakdown.
 */
@Service
public class ApiStatsAppService {

    private final ApiCallStatRepository repository;

    public ApiStatsAppService(ApiCallStatRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ApiStatsDto stats(int days) {
        int window = days <= 0 ? 7 : days;
        Instant since = Instant.now().minus(window, ChronoUnit.DAYS);

        Object[] totals = repository.totalsSince(since);
        long totalCalls = asLong(totals[0]);
        double avgDuration = asDouble(totals[1]);
        long errorCount = asLong(totals[2]);
        double errorRate = totalCalls == 0 ? 0.0d : (double) errorCount / (double) totalCalls;

        List<PathStat> byPath = repository.perPathSince(since).stream()
                .map(row -> {
                    long calls = asLong(row[1]);
                    long pathErrors = asLong(row[3]);
                    double pathErrorRate = calls == 0 ? 0.0d : (double) pathErrors / (double) calls;
                    return new PathStat((String) row[0], calls, asDouble(row[2]), pathErrorRate);
                })
                .toList();

        return new ApiStatsDto(totalCalls, avgDuration, errorRate, byPath);
    }

    private static long asLong(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private static double asDouble(Object value) {
        return value == null ? 0.0d : ((Number) value).doubleValue();
    }
}
