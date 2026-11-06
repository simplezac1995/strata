// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.util.List;

/**
 * Aggregated API call statistics. Populated from a future interceptor/metrics feed; this iteration
 * returns an empty structure (TODO).
 */
public record ApiStatsDto(long totalCalls, double avgDurationMs, double errorRate, List<PathStat> byPath) {

    public record PathStat(String path, long calls, double avgDuration, double errorRate) {}

    public static ApiStatsDto empty() {
        return new ApiStatsDto(0L, 0.0d, 0.0d, List.of());
    }
}
