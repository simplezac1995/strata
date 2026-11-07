// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.repository;

import dev.strata.ai.knowledge.api.entity.AiKnowledge;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiKnowledgeRepository extends JpaRepository<AiKnowledge, Long> {

    List<AiKnowledge> findAllByOrderByIdAsc();
}
