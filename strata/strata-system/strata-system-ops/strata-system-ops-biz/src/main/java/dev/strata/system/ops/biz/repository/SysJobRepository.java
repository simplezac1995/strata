// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.SysJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SysJobRepository extends JpaRepository<SysJob, Long> {

    List<SysJob> findAllByOrderByIdAsc();
}
