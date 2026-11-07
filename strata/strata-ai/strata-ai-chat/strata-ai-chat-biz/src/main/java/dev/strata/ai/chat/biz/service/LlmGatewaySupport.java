// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.service;

import dev.strata.ai.api.request.AiChatAttachment;
import dev.strata.ai.chat.api.entity.AiChatMessage;
import dev.strata.ai.config.api.entity.AiModel;
import dev.strata.ai.config.api.entity.AiProvider;
import dev.strata.plugin.api.llm.LlmChatCommand;
import dev.strata.plugin.api.llm.LlmChatMessage;
import dev.strata.plugin.api.llm.LlmContentPart;
import java.util.ArrayList;
import java.util.List;

public final class LlmGatewaySupport {

    private LlmGatewaySupport() {}

    /** Backward-compatible text-only command assembly. */
    public static LlmChatCommand toCommand(
            AiProvider provider, AiModel model, List<AiChatMessage> history, String userMessage) {
        return toCommand(provider, model, history, userMessage, List.of());
    }

    /** Backward-compatible command assembly without a configured public attachment origin. */
    public static LlmChatCommand toCommand(
            AiProvider provider,
            AiModel model,
            List<AiChatMessage> history,
            String userMessage,
            List<AiChatAttachment> attachments) {
        return toCommand(provider, model, history, userMessage, attachments, null);
    }

    /**
     * Build an OpenAI-compatible command. Image attachments become {@code image_url} content
     * parts; non-image files have no native vision input, so they are surfaced as a
     * {@code [已上传文件] ...} note appended to the end of the user text.
     *
     * @param publicBaseUrl when non-blank, the {@code image_url} each image attachment points at is prefixed
     *     with this public origin so a remote vision model can fetch it; when blank the relative download path
     *     is kept (local development)
     */
    /**
     * Default system prompt used when the admin has not configured one in sys_ai_prompt
     * (code = chat_system). Production deployments should edit the prompt from the
     * Prompts admin page instead of changing this constant.
     */
    static final String DEFAULT_SYSTEM_PROMPT =
            """
            You are the AI assistant for Strata Admin. Help the operator with system management
            tasks, answer questions about the platform, and use the configured tools when available.
            Reply in the user's language (Chinese by default). If you do not know something, say so
            rather than making up facts.
            """;

    public static LlmChatCommand toCommand(
            AiProvider provider,
            AiModel model,
            List<AiChatMessage> history,
            String userMessage,
            List<AiChatAttachment> attachments,
            String publicBaseUrl) {
        return toCommand(provider, model, history, userMessage, attachments, publicBaseUrl, DEFAULT_SYSTEM_PROMPT);
    }

    public static LlmChatCommand toCommand(
            AiProvider provider,
            AiModel model,
            List<AiChatMessage> history,
            String userMessage,
            List<AiChatAttachment> attachments,
            String publicBaseUrl,
            String systemPrompt) {
        String effectiveText = augmentTextWithFiles(userMessage, attachments);
        List<LlmChatMessage> messages = new ArrayList<>();
        messages.add(new LlmChatMessage(
                "system", (systemPrompt == null || systemPrompt.isBlank()) ? DEFAULT_SYSTEM_PROMPT : systemPrompt));
        messages.addAll(history.stream()
                .map(msg -> new LlmChatMessage(msg.getRole(), msg.getContent()))
                .toList());
        return new LlmChatCommand(
                provider.getBaseUrl(),
                provider.getApiKey(),
                model.getCode(),
                messages,
                effectiveText,
                toUserParts(effectiveText, attachments, publicBaseUrl));
    }

    /**
     * Convert the user message plus image attachments into OpenAI multimodal content parts.
     * Returns {@code null} when there are no images (or no attachments at all) so the gateway
     * keeps using the plain-text user message path — the non-image file notes are already
     * baked into {@code userMessage} by {@link #augmentTextWithFiles}.
     */
    static List<LlmContentPart> toUserParts(String userMessage, List<AiChatAttachment> attachments) {
        return toUserParts(userMessage, attachments, null);
    }

    /**
     * Convert the user message plus image attachments into OpenAI multimodal content parts, prefixing each image
     * download URL with {@code publicBaseUrl} when one is configured.
     */
    static List<LlmContentPart> toUserParts(
            String userMessage, List<AiChatAttachment> attachments, String publicBaseUrl) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }
        boolean hasImage = attachments.stream().filter(a -> a != null).anyMatch(LlmGatewaySupport::isImage);
        if (!hasImage) {
            return null;
        }
        List<LlmContentPart> parts = new ArrayList<>();
        if (hasText(userMessage)) {
            parts.add(new LlmContentPart("text", userMessage, null));
        }
        for (AiChatAttachment attachment : attachments) {
            if (attachment == null || !isImage(attachment)) {
                continue;
            }
            parts.add(new LlmContentPart("image_url", null, downloadUrl(attachment, publicBaseUrl)));
        }
        return parts;
    }

    /**
     * Append one {@code [已上传文件] fileName (size 字节)} line per non-image attachment to the
     * end of the user text. Image attachments are left untouched (they become content parts).
     */
    static String augmentTextWithFiles(String userMessage, List<AiChatAttachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return userMessage;
        }
        StringBuilder sb = new StringBuilder(userMessage == null ? "" : userMessage);
        for (AiChatAttachment attachment : attachments) {
            if (attachment == null || isImage(attachment)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append("[已上传文件] ")
                    .append(displayName(attachment))
                    .append(" (")
                    .append(attachment.size() == null ? 0 : attachment.size())
                    .append(" 字节)");
        }
        return sb.toString();
    }

    private static boolean isImage(AiChatAttachment attachment) {
        return attachment.contentType() != null && attachment.contentType().startsWith("image/");
    }

    private static String downloadUrl(AiChatAttachment attachment) {
        return downloadUrl(attachment, null);
    }

    private static String downloadUrl(AiChatAttachment attachment, String publicBaseUrl) {
        String path = "/api/v1/system/files/" + attachment.bucket() + "/" + attachment.key() + "/download";
        if (publicBaseUrl == null || publicBaseUrl.isBlank()) {
            return path;
        }
        String origin = publicBaseUrl.trim();
        while (origin.endsWith("/")) {
            origin = origin.substring(0, origin.length() - 1);
        }
        return origin + path;
    }

    private static String displayName(AiChatAttachment attachment) {
        return hasText(attachment.fileName()) ? attachment.fileName() : attachment.key();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
