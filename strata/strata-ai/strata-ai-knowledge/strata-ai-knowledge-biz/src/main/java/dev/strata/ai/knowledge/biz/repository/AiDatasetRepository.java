// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.repository;

import dev.strata.ai.knowledge.api.entity.AiDataset;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiDatasetRepository extends JpaRepository<AiDataset, Long> {

    List<AiDataset> findAllByOrderByIdAsc();
}
