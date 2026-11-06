// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.GameFlowPointDto;
import dev.strata.system.api.dto.RealtimeDashboardDto;
import dev.strata.system.ops.biz.service.DashboardAppService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardAppService dashboardAppService;

    public DashboardController(DashboardAppService dashboardAppService) {
        this.dashboardAppService = dashboardAppService;
    }

    @GetMapping("/realtime")
    @PreAuthorize(Permissions.SYSTEM_DASHBOARD_LIST)
    public Result<RealtimeDashboardDto> realtime() {
        return Result.ok(dashboardAppService.realtime());
    }

    @GetMapping("/game-flow")
    @PreAuthorize(Permissions.SYSTEM_DASHBOARD_LIST)
    public Result<List<GameFlowPointDto>> gameFlow(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(dashboardAppService.gameFlow(days));
    }
}
