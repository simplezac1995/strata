// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.dto;

public record AiTermDto(String id, String term, String translation, String notes, boolean enabled) {}
