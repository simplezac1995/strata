// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.OperationLogDto;
import dev.strata.system.ops.biz.service.OperationLogAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/operation-logs")
public class OperationLogController {

    private final OperationLogAppService operationLogAppService;

    public OperationLogController(OperationLogAppService operationLogAppService) {
        this.operationLogAppService = operationLogAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_OPERATIONLOG_LIST)
    public Result<PageResult<OperationLogDto>> page(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.ok(operationLogAppService.page(page, size));
    }
}
