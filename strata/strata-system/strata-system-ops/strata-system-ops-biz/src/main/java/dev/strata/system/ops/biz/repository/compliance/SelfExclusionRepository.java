// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository.compliance;

import dev.strata.system.ops.api.entity.compliance.SelfExclusion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SelfExclusionRepository
        extends JpaRepository<SelfExclusion, Long>, JpaSpecificationExecutor<SelfExclusion> {

    List<SelfExclusion> findByUserIdOrderByCreatedAtDesc(Long userId);
}
