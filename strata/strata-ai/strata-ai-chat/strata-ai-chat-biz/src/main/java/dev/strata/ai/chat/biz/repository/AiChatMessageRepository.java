// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.chat.biz.repository;

import dev.strata.ai.chat.api.entity.AiChatMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, Long> {

    List<AiChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);

    /**
     * The {@code limit} most-recent messages of a session, newest first. Used to bound the context window fed
     * back to the LLM (P1-13): an unbounded history grows prompt tokens without end on long sessions. The caller
     * reverses the result to chronological order before building the model command.
     */
    List<AiChatMessage> findBySessionIdOrderByCreatedAtDesc(Long sessionId, Pageable pageable);

    void deleteBySessionId(Long sessionId);
}
