// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.util.List;

/**
 * Read-only JVM / process snapshot (#8). All sizes are bytes.
 *
 * <p>{@code recentResponses} is always empty today: no HTTP response-time collector is wired into the
 * request pipeline yet. TODO(system-monitor): expose a sliding-window p50/p95/p99 from a servlet filter
 * once one exists.
 */
public record MonitorSnapshotDto(
        MemoryDto heap,
        MemoryDto nonHeap,
        List<GcDto> gc,
        ThreadsDto threads,
        PoolDto pool,
        List<RecentResponseDto> recentResponses) {

    public record MemoryDto(long used, long committed, long max) {}

    public record GcDto(String name, long collectionCount, long collectionTimeMs) {}

    public record ThreadsDto(int current, int peak, int daemon, long totalStarted) {}

    /** HikariCP pool metrics; {@code null} when the primary DataSource is not a HikariCP pool. */
    public record PoolDto(String poolName, int active, int idle, int pending, int max, int minIdle) {}

    public record RecentResponseDto(String path, long elapsedMs) {}
}
