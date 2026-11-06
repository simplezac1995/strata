// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.OperationLogDto;
import dev.strata.system.ops.biz.service.OperationLogAppService;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Personal-center view of the current user's own operation log. Lives in the ops module (which owns
 * {@code OperationLogRepository}) but serves the {@code /users/me} path so the frontend can read its own
 * audit trail without an admin operation-log permission.
 */
@Validated
@RestController
@RequestMapping("/api/v1/system/users/me")
public class MyOperationLogController {

    private final OperationLogAppService operationLogAppService;

    public MyOperationLogController(OperationLogAppService operationLogAppService) {
        this.operationLogAppService = operationLogAppService;
    }

    @GetMapping("/operation-logs")
    public Result<PageResult<OperationLogDto>> myOperationLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(operationLogAppService.myLogs(principal.userId(), page, size));
    }
}
