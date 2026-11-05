// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.llm;

import java.util.function.Consumer;

/**
 * LLM gateway SPI. Community ships {@code strata-plugin-llm-openai} (OpenAI-compatible REST).
 * Pro / Enterprise may substitute vendor-specific implementations.
 */
public interface LlmGateway {

    /**
     * Backward-compatible text-only call. Delegates to {@link #chatWithUsage(LlmChatCommand)} and returns only
     * the generated content; token usage is discarded here. New callers that meter usage should prefer
     * {@link #chatWithUsage(LlmChatCommand)}.
     */
    default String chat(LlmChatCommand command) {
        return chatWithUsage(command).content();
    }

    /** Full chat completion including provider-reported token usage. */
    LlmChatResult chatWithUsage(LlmChatCommand command);

    /**
     * Streaming chat: calls the provider with {@code stream=true} and invokes {@code chunkConsumer} for each
     * content delta as it arrives. Returns the accumulated {@link LlmChatResult} when the stream completes.
     * Default implementation falls back to {@link #chatWithUsage} (non-streaming) for providers that don't
     * support SSE.
     */
    default LlmChatResult chatStream(LlmChatCommand command, Consumer<String> chunkConsumer) {
        LlmChatResult result = chatWithUsage(command);
        chunkConsumer.accept(result.content());
        return result;
    }

    default float[] embed(LlmEmbedCommand command) {
        throw new UnsupportedOperationException("embedding not supported by this gateway");
    }
}
