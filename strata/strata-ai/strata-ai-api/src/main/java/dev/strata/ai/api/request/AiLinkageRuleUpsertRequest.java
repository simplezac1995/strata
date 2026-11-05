// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Create/update payload for an AI linkage rule (shared by both since the shape is identical). */
public record AiLinkageRuleUpsertRequest(
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Pattern(regexp = "user|order|recharge|withdraw") String object,
        @NotBlank @Size(max = 64) String event,
        @NotBlank @Pattern(regexp = "notify|tag|email") String action,
        boolean enabled,
        @Size(max = 512) String description) {}
