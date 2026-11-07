// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.repository;

import dev.strata.ai.knowledge.api.entity.AiDocument;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiDocumentRepository extends JpaRepository<AiDocument, Long> {

    List<AiDocument> findAllByKnowledgeIdOrderByIdAsc(Long knowledgeId);

    List<AiDocument> findAllByOrderByIdAsc();

    /** P2-22: cap the un-paged listAll endpoint so it cannot scan the whole table. */
    List<AiDocument> findTop50ByOrderByIdAsc();

    Page<AiDocument> findByKnowledgeId(Long knowledgeId, Pageable pageable);

    long countByKnowledgeId(Long knowledgeId);

    void deleteByKnowledgeId(Long knowledgeId);
}
