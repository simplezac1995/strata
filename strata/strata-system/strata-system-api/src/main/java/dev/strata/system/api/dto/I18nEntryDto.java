// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.dto;

import java.time.Instant;

public record I18nEntryDto(String id, String messageKey, String zh, String en, String updatedBy, Instant updatedAt) {}
