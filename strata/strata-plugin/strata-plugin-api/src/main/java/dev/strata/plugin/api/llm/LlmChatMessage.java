// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.llm;

/** One chat message for {@link LlmGateway}. */
public record LlmChatMessage(String role, String content) {}
