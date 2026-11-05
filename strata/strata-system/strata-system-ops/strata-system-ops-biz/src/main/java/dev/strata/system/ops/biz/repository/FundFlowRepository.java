// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.FundFlow;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FundFlowRepository extends JpaRepository<FundFlow, Long>, JpaSpecificationExecutor<FundFlow> {

    @Query("select coalesce(sum(f.changeAmountMinor), 0) from FundFlow f "
            + "where f.bizType = :bizType and f.createdAt >= :since")
    long sumChangeSince(@Param("bizType") String bizType, @Param("since") Instant since);

    @Query("select count(f) from FundFlow f where f.bizType = :bizType and f.createdAt >= :since")
    long countSince(@Param("bizType") String bizType, @Param("since") Instant since);
}
