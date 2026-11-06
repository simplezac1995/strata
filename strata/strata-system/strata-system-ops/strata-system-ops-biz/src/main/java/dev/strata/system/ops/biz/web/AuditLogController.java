// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.AuditLogDto;
import dev.strata.system.ops.biz.service.AuditLogAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/audit-logs")
public class AuditLogController {

    private final AuditLogAppService auditLogAppService;

    public AuditLogController(AuditLogAppService auditLogAppService) {
        this.auditLogAppService = auditLogAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_AUDITLOG_LIST)
    public Result<PageResult<AuditLogDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(auditLogAppService.page(page, size));
    }
}
