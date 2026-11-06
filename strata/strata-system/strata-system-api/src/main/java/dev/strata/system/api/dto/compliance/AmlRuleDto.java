// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

import java.time.Instant;

public record AmlRuleDto(
        String id,
        String name,
        String ruleType,
        Long thresholdMinor,
        String action,
        boolean enabled,
        Instant createdAt) {}
