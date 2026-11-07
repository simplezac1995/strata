// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.llm.openai;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmContentPart;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleLlmGatewayTest {

    private final OpenAiCompatibleLlmGateway gateway = new OpenAiCompatibleLlmGateway();

    @Test
    void chat_rejects_missing_base_url() {
        BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class,
                () -> gateway.chat(new LlmChatCommand("", "key", "gpt-4o", List.of(), "hello")));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void parseChatResult_extracts_content_and_usage() {
        String raw =
                """
                {
                  "model": "deepseek-chat",
                  "choices": [
                    {
                      "finish_reason": "stop",
                      "message": { "role": "assistant", "content": "Hello\\nWorld \\"quoted\\"" }
                    }
                  ],
                  "usage": { "prompt_tokens": 11, "completion_tokens": 9, "total_tokens": 20 }
                }
                """;

        LlmChatResult result = OpenAiCompatibleLlmGateway.parseChatResult(raw);

        assertThat(result.content()).isEqualTo("Hello\nWorld \"quoted\"");
        assertThat(result.promptTokens()).isEqualTo(11);
        assertThat(result.completionTokens()).isEqualTo(9);
        assertThat(result.totalTokens()).isEqualTo(20);
        assertThat(result.model()).isEqualTo("deepseek-chat");
        assertThat(result.finishReason()).isEqualTo("stop");
    }

    @Test
    void parseChatResult_missing_usage_treats_tokens_as_null() {
        String raw =
                """
                {"model": "m", "choices": [{"message": {"content": "hi"}}]}
                """;

        LlmChatResult result = OpenAiCompatibleLlmGateway.parseChatResult(raw);

        assertThat(result.content()).isEqualTo("hi");
        assertThat(result.promptTokens()).isNull();
        assertThat(result.completionTokens()).isNull();
        assertThat(result.totalTokens()).isNull();
        assertThat(result.finishReason()).isNull();
    }

    @Test
    void parseChatResult_rejects_malformed_payloads() {
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                                BusinessException.class, () -> OpenAiCompatibleLlmGateway.parseChatResult(""))
                        .errorCode())
                .isEqualTo(ErrorCode.INTERNAL_ERROR);

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                                BusinessException.class, () -> OpenAiCompatibleLlmGateway.parseChatResult("not json"))
                        .errorCode())
                .isEqualTo(ErrorCode.INTERNAL_ERROR);

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                                BusinessException.class,
                                () -> OpenAiCompatibleLlmGateway.parseChatResult("{\"choices\": []}"))
                        .errorCode())
                .isEqualTo(ErrorCode.INTERNAL_ERROR);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildUserMessage_text_only_keeps_string_content() {
        LlmChatCommand command = new LlmChatCommand("u", "k", "m", List.of(), "hello");

        Map<String, Object> message = OpenAiCompatibleLlmGateway.buildUserMessage(command);

        assertThat(message.get("role")).isEqualTo("user");
        assertThat(message.get("content")).isEqualTo("hello");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buildUserMessage_multimodal_renders_parts_array() {
        List<LlmContentPart> parts = List.of(
                new LlmContentPart("text", "describe", null),
                new LlmContentPart("image_url", null, "https://cdn/img.png"));
        LlmChatCommand command = new LlmChatCommand("u", "k", "m", List.of(), "describe", parts);

        Map<String, Object> message = OpenAiCompatibleLlmGateway.buildUserMessage(command);

        assertThat(message.get("role")).isEqualTo("user");
        List<Map<String, Object>> rendered = (List<Map<String, Object>>) message.get("content");
        assertThat(rendered).hasSize(2);
        assertThat(rendered.get(0).get("type")).isEqualTo("text");
        assertThat(rendered.get(0).get("text")).isEqualTo("describe");
        assertThat(rendered.get(1).get("type")).isEqualTo("image_url");
        Map<String, Object> imageUrl = (Map<String, Object>) rendered.get(1).get("image_url");
        assertThat(imageUrl.get("url")).isEqualTo("https://cdn/img.png");
    }
}
