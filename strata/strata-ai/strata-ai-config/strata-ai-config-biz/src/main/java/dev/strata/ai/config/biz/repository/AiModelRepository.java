// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.config.biz.repository;

import dev.strata.ai.config.api.entity.AiModel;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiModelRepository extends JpaRepository<AiModel, Long> {

    List<AiModel> findAllByOrderByIdAsc();

    boolean existsByProviderIdAndCode(Long providerId, String code);

    long countByProviderId(Long providerId);

    /**
     * The lowest-id enabled model, without loading every model into memory to filter it (P2-19). Prefer this over
     * {@code findAll().stream().filter(...).findFirst()} on large catalogs.
     */
    Optional<AiModel> findFirstByEnabledTrueOrderByIdAsc();

    /**
     * The lowest-id enabled model belonging to a provider, without loading all of the provider's models.
     */
    Optional<AiModel> findFirstByEnabledTrueAndProviderIdOrderByIdAsc(Long providerId);

    /** The lowest-id enabled model of a given type (e.g. "vision"), for tool routing. */
    Optional<AiModel> findFirstByEnabledTrueAndTypeOrderByIdAsc(String type);
}
