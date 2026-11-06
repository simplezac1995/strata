// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.ApiStatsDto;
import dev.strata.system.ops.biz.service.ApiStatsAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/api-stats")
public class ApiStatsController {

    private final ApiStatsAppService apiStatsAppService;

    public ApiStatsController(ApiStatsAppService apiStatsAppService) {
        this.apiStatsAppService = apiStatsAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.OPS_API_STAT_LIST)
    public Result<ApiStatsDto> stats(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(apiStatsAppService.stats(days));
    }
}
