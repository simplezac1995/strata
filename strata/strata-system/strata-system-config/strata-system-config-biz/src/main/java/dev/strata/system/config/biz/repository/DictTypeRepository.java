// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.repository;

import dev.strata.system.config.api.entity.DictType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictTypeRepository extends JpaRepository<DictType, Long> {

    List<DictType> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByCode(String code);
}
