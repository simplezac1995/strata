// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.repository;

import dev.strata.system.rbac.api.entity.ApiKey;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    List<ApiKey> findByDeletedFalseOrderByIdAsc();

    Page<ApiKey> findByDeletedFalse(Pageable pageable);

    long countByDeletedFalse();

    long countByDeletedFalseAndEnabledTrue();

    Optional<ApiKey> findByIdAndDeletedFalse(Long id);
}
