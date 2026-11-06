// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

public record AiToolRunDto(String tool, String result, boolean usedLlm) {}
