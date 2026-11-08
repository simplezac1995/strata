// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.BackupRecordDto;
import dev.strata.system.api.request.BackupCreateRequest;
import dev.strata.system.ops.biz.service.BackupAppService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/ops/backups")
public class BackupController {

    private final BackupAppService backupAppService;

    public BackupController(BackupAppService backupAppService) {
        this.backupAppService = backupAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.OPS_BACKUP_LIST)
    public Result<PageResult<BackupRecordDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(backupAppService.page(page, size));
    }

    @PostMapping("/create")
    @PreAuthorize(Permissions.OPS_BACKUP_CREATE)
    public Result<BackupRecordDto> create(@Valid @RequestBody BackupCreateRequest request) {
        return Result.ok(backupAppService.create(request));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize(Permissions.OPS_BACKUP_UPDATE)
    public Result<BackupRecordDto> restore(@PathVariable Long id) {
        return Result.ok(backupAppService.restore(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.OPS_BACKUP_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        backupAppService.delete(id);
        return Result.ok();
    }
}
