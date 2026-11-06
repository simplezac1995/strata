// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.service;

import dev.strata.ai.knowledge.api.dto.AiDocumentChunkDto;
import dev.strata.ai.knowledge.api.entity.AiDocumentChunk;
import dev.strata.ai.knowledge.biz.repository.AiDocumentChunkRepository;
import dev.strata.common.core.web.PageResult;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read model for the chunk browser (RAG observability).
 *
 * <p>The controller must not touch the JPA entity (the architecture rule forbids entity dependencies in controller
 * classes), so the repository query and the entity-to-DTO mapping live here.
 */
@Service
public class AiDocumentChunkQueryService {

    private static final int CONTENT_PREVIEW_LENGTH = 300;

    private final AiDocumentChunkRepository chunks;

    public AiDocumentChunkQueryService(AiDocumentChunkRepository chunks) {
        this.chunks = chunks;
    }

    /** All chunks of one document, in reading order; other documents fall back to the paged newest-first view. */
    @Transactional(readOnly = true)
    public PageResult<AiDocumentChunkDto> page(Long documentId, int page, int size) {
        if (documentId != null) {
            var rows = chunks.findByDocumentIdOrderByChunkIndexAsc(documentId).stream()
                    .map(AiDocumentChunkQueryService::toDto)
                    .toList();
            return new PageResult<>(rows, 0, rows.size(), (long) rows.size(), 1);
        }
        var result = chunks.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        var rows = result.getContent().stream()
                .map(AiDocumentChunkQueryService::toDto)
                .toList();
        return new PageResult<>(rows, page, size, result.getTotalElements(), result.getTotalPages());
    }

    private static AiDocumentChunkDto toDto(AiDocumentChunk chunk) {
        String content = chunk.getContent() == null ? "" : chunk.getContent();
        return new AiDocumentChunkDto(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getChunkIndex(),
                content.substring(0, Math.min(CONTENT_PREVIEW_LENGTH, content.length())),
                content.length());
    }
}
