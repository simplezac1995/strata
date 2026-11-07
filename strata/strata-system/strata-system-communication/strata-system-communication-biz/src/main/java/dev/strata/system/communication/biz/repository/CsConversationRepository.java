// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.repository;

import dev.strata.system.communication.api.entity.CsConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CsConversationRepository extends JpaRepository<CsConversation, Long> {

    Page<CsConversation> findByStatusAndDeletedFalse(String status, Pageable pageable);

    Page<CsConversation> findByDeletedFalse(Pageable pageable);

    /** The player's own ticket list, newest first. */
    Page<CsConversation> findByPlayerIdAndDeletedFalseOrderByLastMessageAtDesc(Long playerId, Pageable pageable);
}
