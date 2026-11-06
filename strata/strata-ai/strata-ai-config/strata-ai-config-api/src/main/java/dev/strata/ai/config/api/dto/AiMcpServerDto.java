// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.api.dto;

public record AiMcpServerDto(String id, String name, String url, String authType, String authToken, boolean enabled) {}
