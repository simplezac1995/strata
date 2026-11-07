// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiSkill;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiSkillRepository extends JpaRepository<AiSkill, Long> {

    List<AiSkill> findAllByOrderByIdAsc();

    Page<AiSkill> findByType(String type, Pageable pageable);
}
