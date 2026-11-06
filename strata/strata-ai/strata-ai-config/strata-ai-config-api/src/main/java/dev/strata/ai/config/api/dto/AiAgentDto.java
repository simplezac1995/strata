// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.dto;

public record AiAgentDto(String id, String name, String model, String prompt, boolean enabled) {}
