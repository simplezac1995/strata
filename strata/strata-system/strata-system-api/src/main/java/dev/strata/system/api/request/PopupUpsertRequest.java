// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record PopupUpsertRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 512) String imageUrl,
        @Size(max = 512) String linkUrl,
        Instant startAt,
        Instant endAt,
        @NotBlank @Size(max = 16) String frequency,
        Boolean enabled) {}
