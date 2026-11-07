// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record OpsAnnouncementUpsertRequest(
        @NotBlank @Size(max = 128) String title,
        String content,
        @NotBlank @Size(max = 16) String type,
        Instant startAt,
        Instant endAt,
        @NotBlank @Size(max = 16) String status,
        Integer sort) {}
