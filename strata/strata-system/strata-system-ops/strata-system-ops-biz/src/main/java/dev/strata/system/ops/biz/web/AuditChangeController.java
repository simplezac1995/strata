// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.AuditLogChangeDto;
import dev.strata.system.ops.biz.service.AuditLogChangeAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/audit/changes")
public class AuditChangeController {

    private final AuditLogChangeAppService auditLogChangeAppService;

    public AuditChangeController(AuditLogChangeAppService auditLogChangeAppService) {
        this.auditLogChangeAppService = auditLogChangeAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_AUDIT_LIST)
    public Result<PageResult<AuditLogChangeDto>> page(
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(auditLogChangeAppService.page(entity, entityId, p, s));
    }

    @PostMapping("/{id}/rollback")
    @PreAuthorize(Permissions.SYSTEM_AUDIT_UPDATE)
    public Result<AuditLogChangeDto> rollback(@PathVariable Long id) {
        return Result.ok(auditLogChangeAppService.rollback(id));
    }
}
