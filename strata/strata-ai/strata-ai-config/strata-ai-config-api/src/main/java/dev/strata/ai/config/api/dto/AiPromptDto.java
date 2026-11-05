// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.dto;

public record AiPromptDto(String id, String code, String name, String template, boolean enabled) {}
