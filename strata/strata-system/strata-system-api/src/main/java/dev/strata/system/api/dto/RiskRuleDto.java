// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record RiskRuleDto(
        String id, String name, String type, int threshold, String action, boolean enabled, Instant createdAt) {}
