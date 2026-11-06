// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiLabelSample;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiLabelSampleRepository extends JpaRepository<AiLabelSample, Long> {

    Page<AiLabelSample> findByTaskId(Long taskId, Pageable pageable);

    long countByTaskId(Long taskId);
}
