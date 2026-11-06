// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.repository;

import dev.strata.ai.knowledge.api.entity.AiVectorStore;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiVectorStoreRepository extends JpaRepository<AiVectorStore, Long> {

    List<AiVectorStore> findAllByOrderByIdAsc();
}
