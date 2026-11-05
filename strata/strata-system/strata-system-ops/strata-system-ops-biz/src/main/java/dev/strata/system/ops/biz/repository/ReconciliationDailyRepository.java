// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.ReconciliationDaily;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReconciliationDailyRepository
        extends JpaRepository<ReconciliationDaily, Long>, JpaSpecificationExecutor<ReconciliationDaily> {

    Optional<ReconciliationDaily> findByBizDate(LocalDate bizDate);
}
