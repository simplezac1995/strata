// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiAgentNode;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAgentNodeRepository extends JpaRepository<AiAgentNode, Long> {

    List<AiAgentNode> findByAgentIdOrderByIdAsc(Long agentId);

    void deleteByAgentId(Long agentId);
}
