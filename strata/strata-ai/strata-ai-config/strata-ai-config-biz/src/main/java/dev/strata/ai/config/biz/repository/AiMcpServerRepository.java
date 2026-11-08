// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiMcpServer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMcpServerRepository extends JpaRepository<AiMcpServer, Long> {

    List<AiMcpServer> findAllByOrderByIdAsc();
}
