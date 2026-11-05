// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.LoginLog;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long>, JpaSpecificationExecutor<LoginLog> {

    /** Distinct successful-login username count since {@code from} (#6 "active today" proxy). */
    long countDistinctUsernameByStatusAndCreatedAtGreaterThanEqual(String status, Instant from);
}
