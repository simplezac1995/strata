// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Create a chat message. {@code content} is the plain-text part and may be blank when the user sends
 * attachments only; the service enforces that at least one of {@code content} / {@code attachments} is
 * present.
 */
public record AiChatMessageCreateRequest(@Size(max = 8000) String content, List<AiChatAttachment> attachments) {}
