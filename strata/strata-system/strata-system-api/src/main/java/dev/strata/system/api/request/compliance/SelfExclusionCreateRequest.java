// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request.compliance;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * A null {@code days} means permanent self-exclusion (no end date) — the safe default, mirroring ADR-0033.
 */
public record SelfExclusionCreateRequest(@Positive Integer days, @Size(max = 512) String reason) {}
