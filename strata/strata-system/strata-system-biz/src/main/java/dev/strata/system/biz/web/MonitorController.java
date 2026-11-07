// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.MonitorSnapshotDto;
import dev.strata.system.biz.monitor.MonitorAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/system/monitor")
public class MonitorController {

    private final MonitorAppService monitorAppService;

    public MonitorController(MonitorAppService monitorAppService) {
        this.monitorAppService = monitorAppService;
    }

    @GetMapping("/snapshot")
    @PreAuthorize(Permissions.SYSTEM_MONITOR_LIST)
    public Result<MonitorSnapshotDto> snapshot() {
        return Result.ok(monitorAppService.snapshot());
    }
}
