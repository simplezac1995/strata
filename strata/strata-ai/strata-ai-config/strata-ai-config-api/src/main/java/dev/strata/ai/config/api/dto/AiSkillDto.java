// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.dto;

public record AiSkillDto(String id, String name, String type, String path, String description, boolean enabled) {}
