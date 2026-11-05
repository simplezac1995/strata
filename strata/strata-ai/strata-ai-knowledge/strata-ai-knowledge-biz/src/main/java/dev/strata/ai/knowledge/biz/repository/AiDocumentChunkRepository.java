// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.repository;

import dev.strata.ai.knowledge.api.entity.AiDocumentChunk;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiDocumentChunkRepository extends JpaRepository<AiDocumentChunk, Long> {

    List<AiDocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);

    List<AiDocumentChunk> findByDocumentIdInOrderByChunkIndexAsc(Collection<Long> documentIds);

    /** Global (no knowledge-base scoping) retrieval is capped so a QA call cannot scan the whole chunk table. */
    List<AiDocumentChunk> findTop500ByOrderByIdAsc();

    long countByDocumentId(Long documentId);

    void deleteByDocumentId(Long documentId);
}
