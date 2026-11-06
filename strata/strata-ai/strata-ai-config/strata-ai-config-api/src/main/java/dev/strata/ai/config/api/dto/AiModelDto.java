// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.dto;

public record AiModelDto(String id, String providerId, String code, String name, String type, boolean enabled) {}
