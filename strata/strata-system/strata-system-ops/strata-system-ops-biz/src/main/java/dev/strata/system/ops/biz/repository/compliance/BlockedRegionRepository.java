// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository.compliance;

import dev.strata.system.ops.api.entity.compliance.BlockedRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BlockedRegionRepository
        extends JpaRepository<BlockedRegion, Long>, JpaSpecificationExecutor<BlockedRegion> {

    boolean existsByCountryCode(String countryCode);
}
