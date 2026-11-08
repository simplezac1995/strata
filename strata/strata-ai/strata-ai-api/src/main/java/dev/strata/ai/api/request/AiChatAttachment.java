// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.api.request;

/**
 * Reference to a user-uploaded attachment attached to an AI chat message. The file itself was already
 * uploaded through the system file API; this only carries its location so the chat layer can forward it to
 * the model.
 */
public record AiChatAttachment(String bucket, String key, String contentType, String fileName, Long size) {}
