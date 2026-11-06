// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.config.biz.repository;

import dev.strata.system.config.api.entity.Region;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findAllByOrderByLevelAscIdAsc();

    boolean existsByCode(String code);

    long countByParentId(Long parentId);
}
