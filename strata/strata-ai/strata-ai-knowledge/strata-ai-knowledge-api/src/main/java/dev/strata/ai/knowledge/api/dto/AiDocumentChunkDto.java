// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.api.dto;

/**
 * One document chunk as the admin console browses it (RAG observability).
 *
 * <p>A contract record rather than the JPA entity: {@code content} is truncated for the list view, so exposing the
 * entity would both leak the full stored text and drag JPA into the controller's signature.
 */
public record AiDocumentChunkDto(Long id, Long documentId, Integer chunkIndex, String content, int contentLength) {}
