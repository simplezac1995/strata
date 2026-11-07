// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.repository;

import dev.strata.system.ops.api.entity.DailyTask;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyTaskRepository extends JpaRepository<DailyTask, Long> {

    List<DailyTask> findAllByOrderByIdAsc();
}
