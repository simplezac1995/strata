// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record DailyTaskUpsertRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 16) String taskType,
        @PositiveOrZero int targetCount,
        @PositiveOrZero long rewardPoints) {}
