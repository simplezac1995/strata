// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderRepository extends JpaRepository<AiProvider, Long> {

    boolean existsByCode(String code);

    /**
     * The lowest-id enabled provider, without loading every provider into memory to filter it (P2-19). Prefer this
     * over {@code findAll().stream().filter(...).findFirst()} on large catalogs.
     */
    Optional<AiProvider> findFirstByEnabledTrueOrderByIdAsc();
}
