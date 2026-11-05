// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.JobDto;
import dev.strata.system.api.request.JobCreateRequest;
import dev.strata.system.api.request.JobUpdateRequest;
import dev.strata.system.ops.biz.service.JobAppService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/jobs")
public class JobController {

    private final JobAppService jobAppService;

    public JobController(JobAppService jobAppService) {
        this.jobAppService = jobAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_JOB_LIST)
    public Result<List<JobDto>> list() {
        return Result.ok(jobAppService.list());
    }

    @GetMapping("/page")
    @PreAuthorize(Permissions.SYSTEM_JOB_LIST)
    public Result<PageResult<JobDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(jobAppService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_JOB_LIST)
    public Result<JobDto> get(@PathVariable Long id) {
        return Result.ok(jobAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_JOB_CREATE)
    public Result<JobDto> create(@Valid @RequestBody JobCreateRequest request) {
        return Result.ok(jobAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_JOB_UPDATE)
    public Result<JobDto> update(@PathVariable Long id, @Valid @RequestBody JobUpdateRequest request) {
        return Result.ok(jobAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_JOB_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        jobAppService.delete(id);
        return Result.ok();
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize(Permissions.SYSTEM_JOB_UPDATE)
    public Result<JobDto> pause(@PathVariable Long id) {
        return Result.ok(jobAppService.pause(id));
    }

    @PostMapping("/{id}/run")
    @PreAuthorize(Permissions.SYSTEM_JOB_UPDATE)
    public Result<JobDto> run(@PathVariable Long id) {
        return Result.ok(jobAppService.run(id));
    }
}
