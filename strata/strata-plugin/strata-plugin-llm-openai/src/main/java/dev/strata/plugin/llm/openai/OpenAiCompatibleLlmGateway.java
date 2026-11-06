// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.llm.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmChatResult;
import dev.strata.plugin.api.llm.LlmContentPart;
import dev.strata.plugin.api.llm.LlmEmbedCommand;
import dev.strata.plugin.api.llm.LlmGateway;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Minimal OpenAI-compatible {@code /chat/completions} client. */
public class OpenAiCompatibleLlmGateway implements LlmGateway {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RestClient.Builder restClientBuilder;

    public OpenAiCompatibleLlmGateway() {
        this.restClientBuilder = RestClient.builder();
    }

    @Override
    public LlmChatResult chatWithUsage(LlmChatCommand command) {
        if (command.baseUrl() == null || command.baseUrl().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI provider base URL is not configured");
        }
        if (command.apiKey() == null || command.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI provider API key is not configured");
        }
        if (command.modelCode() == null || command.modelCode().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "No enabled AI model configured");
        }

        String base = command.baseUrl().endsWith("/")
                ? command.baseUrl().substring(0, command.baseUrl().length() - 1)
                : command.baseUrl();
        String url = base + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", command.modelCode());
        List<Map<String, Object>> messages = command.history().stream()
                .map(msg -> Map.<String, Object>of("role", msg.role(), "content", msg.content()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        messages.add(buildUserMessage(command));
        body.put("messages", messages);

        try {
            String raw = restClientBuilder
                    .build()
                    .post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + command.apiKey())
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseChatResult(raw);
        } catch (RestClientException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM provider call failed: " + ex.getMessage());
        }
    }

    @Override
    public LlmChatResult chatStream(LlmChatCommand command, Consumer<String> chunkConsumer) {
        if (command.baseUrl() == null || command.baseUrl().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI provider base URL is not configured");
        }
        if (command.apiKey() == null || command.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "AI provider API key is not configured");
        }
        if (command.modelCode() == null || command.modelCode().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "No enabled AI model configured");
        }

        String base = command.baseUrl().endsWith("/")
                ? command.baseUrl().substring(0, command.baseUrl().length() - 1)
                : command.baseUrl();
        String url = base + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", command.modelCode());
        body.put("stream", true);
        // OpenAI-compatible providers only emit a final usage chunk when this is set.
        body.put("stream_options", Map.of("include_usage", true));
        List<Map<String, Object>> messages = command.history().stream()
                .map(msg -> Map.<String, Object>of("role", msg.role(), "content", msg.content()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        messages.add(buildUserMessage(command));
        body.put("messages", messages);

        StringBuilder full = new StringBuilder();
        String model = null;
        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;
        try (InputStream is = restClientBuilder
                        .build()
                        .post()
                        .uri(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + command.apiKey())
                        .body(body)
                        .retrieve()
                        .body(InputStream.class);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;
                if (data.isEmpty()) continue;
                JsonNode node = MAPPER.readTree(data);
                if (model == null && node.hasNonNull("model"))
                    model = node.get("model").asText();
                JsonNode choices = node.path("choices");
                if (choices.isArray() && !choices.isEmpty()) {
                    String delta = choices.get(0).path("delta").path("content").asText("");
                    if (!delta.isEmpty()) {
                        full.append(delta);
                        chunkConsumer.accept(delta);
                    }
                }
                JsonNode usage = node.path("usage");
                if (usage != null && !usage.isMissingNode() && !usage.isNull()) {
                    promptTokens = optionalInt(usage, "prompt_tokens");
                    completionTokens = optionalInt(usage, "completion_tokens");
                    totalTokens = optionalInt(usage, "total_tokens");
                }
            }
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM stream failed: " + ex.getMessage());
        }
        if (full.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM stream returned empty content");
        }
        return new LlmChatResult(full.toString(), promptTokens, completionTokens, totalTokens, model, "stop");
    }

    static Map<String, Object> buildUserMessage(LlmChatCommand command) {
        List<LlmContentPart> parts = command.userParts();
        if (parts == null || parts.isEmpty()) {
            // Backward-compatible plain-text user message.
            return Map.of("role", "user", "content", command.userMessage());
        }
        List<Object> rendered = new ArrayList<>();
        for (LlmContentPart part : parts) {
            if (part == null) {
                continue;
            }
            if ("image_url".equals(part.type())) {
                rendered.add(Map.of(
                        "type",
                        "image_url",
                        "image_url",
                        Map.of("url", part.imageUrl() == null ? "" : part.imageUrl())));
            } else {
                rendered.add(Map.of("type", "text", "text", part.text() == null ? "" : part.text()));
            }
        }
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", rendered);
        return message;
    }

    /**
     * Parse a full OpenAI-compatible chat completion JSON into a {@link LlmChatResult}. Uses Jackson (not
     * regex) so embedded quotes / newlines in {@code content} and the {@code usage} block are extracted
     * faithfully.
     */
    static LlmChatResult parseChatResult(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Empty response from LLM provider");
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(raw);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM response is not valid JSON: " + ex.getMessage());
        }
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM response missing choices");
        }
        JsonNode first = choices.get(0);
        String content = first.path("message").path("content").asText(null);
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM response missing message content");
        }
        String finishReason =
                first.hasNonNull("finish_reason") ? first.get("finish_reason").asText() : null;
        String model = root.hasNonNull("model") ? root.get("model").asText() : null;

        JsonNode usage = root.path("usage");
        Integer promptTokens = optionalInt(usage, "prompt_tokens");
        Integer completionTokens = optionalInt(usage, "completion_tokens");
        Integer totalTokens = optionalInt(usage, "total_tokens");
        return new LlmChatResult(content, promptTokens, completionTokens, totalTokens, model, finishReason);
    }

    private static Integer optionalInt(JsonNode usage, String field) {
        if (usage == null || usage.isMissingNode() || usage.isNull()) {
            return null;
        }
        JsonNode node = usage.get(field);
        if (node == null || node.isNull() || !node.canConvertToInt()) {
            return null;
        }
        return node.asInt();
    }

    @Override
    public float[] embed(LlmEmbedCommand command) {
        try {
            String url = command.baseUrl().replaceAll("/+$", "") + "/embeddings";
            var body = MAPPER.createObjectNode();
            body.put("model", command.modelCode());
            body.put("input", command.text());
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(java.net.URI.create(url))
                    .header("Authorization", "Bearer " + command.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                    .build();
            var client = java.net.http.HttpClient.newHttpClient();
            var resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() / 100 != 2) {
                throw new RuntimeException("embedding failed: " + resp.statusCode() + " " + resp.body());
            }
            JsonNode root = MAPPER.readTree(resp.body());
            JsonNode emb = root.path("data").path(0).path("embedding");
            float[] out = new float[emb.size()];
            for (int i = 0; i < emb.size(); i++) {
                out[i] = (float) emb.get(i).asDouble();
            }
            return out;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("embedding call failed", e);
        }
    }
}
