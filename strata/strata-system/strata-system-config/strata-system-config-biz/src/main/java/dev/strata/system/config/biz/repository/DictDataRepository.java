// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.repository;

import dev.strata.system.config.api.entity.DictData;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DictDataRepository extends JpaRepository<DictData, Long> {

    List<DictData> findAllByTypeIdOrderBySortOrderAscIdAsc(Long typeId);

    long countByTypeId(Long typeId);

    void deleteByTypeId(Long typeId);

    boolean existsByTypeIdAndValue(Long typeId, String value);
}
