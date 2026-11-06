// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.AuditLogChange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogChangeRepository
        extends JpaRepository<AuditLogChange, Long>, JpaSpecificationExecutor<AuditLogChange> {}
