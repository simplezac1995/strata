// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.llm;

/**
 * Full result of an {@link LlmGateway} chat completion: the generated text plus the token-usage metadata
 * reported by the provider. The token counts may be {@code null} when the provider omitted the {@code usage}
 * block; callers that bill or meter usage should treat {@code null} as {@code 0}.
 *
 * @param content generated assistant message content
 * @param promptTokens tokens used for the prompt, or {@code null} when not reported
 * @param completionTokens tokens used for the completion, or {@code null} when not reported
 * @param totalTokens total tokens (prompt + completion), or {@code null} when not reported
 * @param model model that produced the completion (echoed from the response)
 * @param finishReason why the model stopped ({@code stop}/{@code length}/...), may be {@code null}
 */
public record LlmChatResult(
        String content,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String model,
        String finishReason) {

    /** Backward-compatible content-only view when usage is irrelevant. */
    public int promptTokensOrZero() {
        return promptTokens == null ? 0 : promptTokens;
    }

    public int completionTokensOrZero() {
        return completionTokens == null ? 0 : completionTokens;
    }

    public int totalTokensOrZero() {
        return totalTokens == null ? 0 : totalTokens;
    }
}
