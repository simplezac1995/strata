// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record PopupConfigDto(
        String id,
        String name,
        String imageUrl,
        String linkUrl,
        Instant startAt,
        Instant endAt,
        String frequency,
        boolean enabled,
        Instant createdAt) {}
