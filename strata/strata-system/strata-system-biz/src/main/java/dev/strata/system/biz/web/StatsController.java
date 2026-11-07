// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.StatsOverviewDto;
import dev.strata.system.api.dto.StatsRetentionDto;
import dev.strata.system.api.dto.StatsTrendDto;
import dev.strata.system.biz.stats.StatsAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/stats")
public class StatsController {

    private final StatsAppService statsAppService;

    public StatsController(StatsAppService statsAppService) {
        this.statsAppService = statsAppService;
    }

    @GetMapping("/overview")
    @PreAuthorize(Permissions.SYSTEM_STATS_LIST)
    public Result<StatsOverviewDto> overview(@RequestParam(defaultValue = "7d") String range) {
        return Result.ok(statsAppService.overview(range));
    }

    @GetMapping("/trend")
    @PreAuthorize(Permissions.SYSTEM_STATS_LIST)
    public Result<StatsTrendDto> trend(
            @RequestParam(defaultValue = "user") String metric, @RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsAppService.trend(metric, days));
    }

    @GetMapping("/retention")
    @PreAuthorize(Permissions.SYSTEM_STATS_LIST)
    public Result<StatsRetentionDto> retention(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsAppService.retention(days));
    }
}
