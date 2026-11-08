// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto.compliance;

public record GeoCheckResult(boolean blocked, String action, String reason) {}
