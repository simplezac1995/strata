// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.llm;

import java.util.List;

/** OpenAI-compatible chat completion request (provider credentials + messages). */
public record LlmChatCommand(
        String baseUrl,
        String apiKey,
        String modelCode,
        List<LlmChatMessage> history,
        String userMessage,
        List<LlmContentPart> userParts) {

    /** Backward-compatible text-only constructor (no multimodal parts). */
    public LlmChatCommand(
            String baseUrl, String apiKey, String modelCode, List<LlmChatMessage> history, String userMessage) {
        this(baseUrl, apiKey, modelCode, history, userMessage, null);
    }
}
