// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.LoginLogDto;
import dev.strata.system.api.dto.LoginLogQuery;
import dev.strata.system.ops.biz.service.LoginLogAppService;
import java.time.Instant;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/login-logs")
public class LoginLogController {

    private final LoginLogAppService loginLogAppService;

    public LoginLogController(LoginLogAppService loginLogAppService) {
        this.loginLogAppService = loginLogAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_LOGINLOG_LIST)
    public Result<PageResult<LoginLogDto>> page(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String ip,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(loginLogAppService.page(new LoginLogQuery(username, status, ip, from, to, page, size)));
    }
}
