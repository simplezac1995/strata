// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.DailyTaskDto;
import dev.strata.system.api.request.DailyTaskUpsertRequest;
import dev.strata.system.ops.biz.service.DailyTaskAppService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/ops/daily-tasks")
public class DailyTaskController {

    private final DailyTaskAppService dailyTaskAppService;

    public DailyTaskController(DailyTaskAppService dailyTaskAppService) {
        this.dailyTaskAppService = dailyTaskAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.OPS_DAILY_TASK_LIST)
    public Result<List<DailyTaskDto>> list() {
        return Result.ok(dailyTaskAppService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.OPS_DAILY_TASK_LIST)
    public Result<DailyTaskDto> get(@PathVariable Long id) {
        return Result.ok(dailyTaskAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.OPS_DAILY_TASK_CREATE)
    public Result<DailyTaskDto> create(@Valid @RequestBody DailyTaskUpsertRequest request) {
        return Result.ok(dailyTaskAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.OPS_DAILY_TASK_UPDATE)
    public Result<DailyTaskDto> update(@PathVariable Long id, @Valid @RequestBody DailyTaskUpsertRequest request) {
        return Result.ok(dailyTaskAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.OPS_DAILY_TASK_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        dailyTaskAppService.delete(id);
        return Result.ok();
    }
}
