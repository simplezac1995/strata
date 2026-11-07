// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.repository;

import dev.strata.ai.chat.api.entity.AiChatSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatSessionRepository extends JpaRepository<AiChatSession, Long> {

    List<AiChatSession> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Most-recent {@code 100} sessions for a user; caps the unscoped list endpoint (P2-22). */
    List<AiChatSession> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<AiChatSession> findByIdAndUserId(Long id, Long userId);
}
