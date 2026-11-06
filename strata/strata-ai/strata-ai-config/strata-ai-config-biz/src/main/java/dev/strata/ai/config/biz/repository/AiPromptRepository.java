// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiPrompt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiPromptRepository extends JpaRepository<AiPrompt, Long> {

    List<AiPrompt> findAllByOrderByIdAsc();

    boolean existsByCode(String code);

    Optional<AiPrompt> findByCodeAndEnabledTrue(String code);
}
