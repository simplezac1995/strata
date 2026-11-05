// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record RiskIpBlacklistCreateRequest(
        @NotBlank @Size(max = 64) String ipAddress, @Size(max = 512) String reason, Instant expiresAt) {}
