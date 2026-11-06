// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiTerm;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiTermRepository extends JpaRepository<AiTerm, Long> {

    List<AiTerm> findAllByOrderByIdAsc();

    List<AiTerm> findAllByEnabledTrueOrderByIdAsc();

    boolean existsByTerm(String term);
}
