// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record JobDto(
        String id,
        String name,
        String jobGroup,
        String cron,
        String status,
        Instant lastRun,
        Instant nextRun,
        boolean enabled) {}
