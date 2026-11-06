// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.repository;

import dev.strata.system.config.api.entity.SysParam;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysParamRepository extends JpaRepository<SysParam, Long> {

    List<SysParam> findAllByOrderByIdAsc();

    Page<SysParam> findByKeyContainingIgnoreCase(String key, Pageable pageable);

    boolean existsByKey(String key);
}
