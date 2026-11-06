// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.JobDto;
import dev.strata.system.api.dto.ScheduledTaskLogDto;
import dev.strata.system.ops.biz.service.JobAppService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ops-facing scheduled task management on top of the existing {@code SysJob} ({@code system:job}).
 * The backing entity already ships, so no {@code schedule_task_log} table is created; the logs endpoint
 * returns an empty list until a real execution-log table lands.
 */
@RestController
@RequestMapping("/api/v1/ops/scheduled-tasks")
public class ScheduledTaskController {

    private final JobAppService jobAppService;

    public ScheduledTaskController(JobAppService jobAppService) {
        this.jobAppService = jobAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.OPS_SCHEDULED_TASK_LIST)
    public Result<List<JobDto>> list() {
        return Result.ok(jobAppService.list());
    }

    @PostMapping("/{id}/trigger")
    @PreAuthorize(Permissions.OPS_SCHEDULED_TASK_UPDATE)
    public Result<JobDto> trigger(@PathVariable Long id) {
        return Result.ok(jobAppService.run(id));
    }

    @PutMapping("/{id}/toggle")
    @PreAuthorize(Permissions.OPS_SCHEDULED_TASK_UPDATE)
    public Result<JobDto> toggle(@PathVariable Long id) {
        return Result.ok(jobAppService.toggle(id));
    }

    @GetMapping("/{id}/logs")
    @PreAuthorize(Permissions.OPS_SCHEDULED_TASK_LIST)
    public Result<List<ScheduledTaskLogDto>> logs(@PathVariable Long id) {
        // TODO: return execution history once a schedule_task_log table exists.
        return Result.ok(List.of());
    }
}
