// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.api.dto;

public record AiDatasetDto(String id, String name, String description, String source, int records, boolean enabled) {}
