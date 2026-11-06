// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.repository;

import dev.strata.ai.biz.entity.AiLlmUsage;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence for the {@code ai_llm_usage} ledger rows. */
public interface AiLlmUsageRepository extends JpaRepository<AiLlmUsage, Long> {

    /**
     * All usage rows at or after {@code since}, oldest first. Billing / usage aggregation reads this bounded
     * window and groups by day / model / session in Java, keeping calendar-zone handling explicit.
     */
    List<AiLlmUsage> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(Instant since);
}
