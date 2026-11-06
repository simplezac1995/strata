// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.knowledge.biz.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

/** Find the enabled embedding model + its provider credentials for RAG vector indexing. */
public interface EmbeddingModelFinder extends CrudRepository<dev.strata.ai.config.api.entity.AiModel, Long> {

    record EmbeddingConfig(String modelCode, String baseUrl, String apiKey) {}

    @Query(
            nativeQuery = true,
            value = "SELECT m.code AS model_code, p.base_url AS base_url, p.api_key AS api_key "
                    + "FROM sys_ai_model m JOIN sys_ai_provider p ON p.id = m.provider_id "
                    + "WHERE m.enabled = 1 AND m.type = 'embedding' AND p.enabled = 1 "
                    + "ORDER BY m.id ASC LIMIT 1")
    Optional<EmbeddingConfig> findEnabledEmbeddingConfig();
}
