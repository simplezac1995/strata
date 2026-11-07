// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.dto;

/** Provider-reported token usage for a single LLM call, returned alongside generated content. */
public record AiTokenUsageDto(long promptTokens, long completionTokens, long totalTokens) {}
