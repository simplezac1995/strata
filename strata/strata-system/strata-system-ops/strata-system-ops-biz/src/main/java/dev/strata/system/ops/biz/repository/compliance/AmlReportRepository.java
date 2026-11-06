// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository.compliance;

import dev.strata.system.ops.api.entity.compliance.AmlReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AmlReportRepository extends JpaRepository<AmlReport, Long>, JpaSpecificationExecutor<AmlReport> {}
