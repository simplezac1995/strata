// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record SiteSettingDto(String key, String value, Instant updatedAt) {}
