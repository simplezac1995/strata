// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiLabelTask;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiLabelTaskRepository extends JpaRepository<AiLabelTask, Long> {

    Page<AiLabelTask> findByDeletedFalse(Pageable pageable);

    Optional<AiLabelTask> findByIdAndDeletedFalse(Long id);
}
