// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository.compliance;

import dev.strata.system.ops.api.entity.compliance.ResponsibleGamblingLimit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ResponsibleGamblingLimitRepository
        extends JpaRepository<ResponsibleGamblingLimit, Long>, JpaSpecificationExecutor<ResponsibleGamblingLimit> {

    List<ResponsibleGamblingLimit> findByUserIdOrderByCreatedAtDesc(Long userId);
}
