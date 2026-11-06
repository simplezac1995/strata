// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    Page<OperationLog> findByOperatorId(Long operatorId, Pageable pageable);
}
