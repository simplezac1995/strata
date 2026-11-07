// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

/**
 * One scheduled task execution log entry. No log table exists yet (the backing task entity already
 * ships), so the logs endpoint returns an empty list this iteration (TODO).
 */
public record ScheduledTaskLogDto(
        String id,
        String taskName,
        String status,
        long durationMs,
        String message,
        Instant startedAt,
        Instant finishedAt) {}
