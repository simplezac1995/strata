// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record AnnouncementDto(
        String id, String title, String content, boolean enabled, Instant createdAt, Instant updatedAt) {}
