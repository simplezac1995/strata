// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiAgent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAgentRepository extends JpaRepository<AiAgent, Long> {

    List<AiAgent> findAllByOrderByIdAsc();
}
