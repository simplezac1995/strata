// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiAgentRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAgentRunRepository extends JpaRepository<AiAgentRun, Long> {}
