// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

/**
 * Operator broadcast announcement (notice | popup | banner). Distinct from the simpler
 * communication {@code AnnouncementDto} ({@code sys_announcement}); this one carries a schedule window.
 */
public record OpsAnnouncementDto(
        String id,
        String title,
        String content,
        String type,
        Instant startAt,
        Instant endAt,
        String status,
        int sort,
        Instant createdAt) {}
