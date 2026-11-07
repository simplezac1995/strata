// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.RiskAlertDto;
import dev.strata.system.ops.biz.service.RiskAlertAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/risk/alerts")
public class RiskAlertController {

    private final RiskAlertAppService riskAlertAppService;

    public RiskAlertController(RiskAlertAppService riskAlertAppService) {
        this.riskAlertAppService = riskAlertAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_RISK_LIST)
    public Result<PageResult<RiskAlertDto>> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(riskAlertAppService.page(status, p, s));
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize(Permissions.SYSTEM_RISK_UPDATE)
    public Result<RiskAlertDto> resolve(@PathVariable Long id) {
        return Result.ok(riskAlertAppService.resolve(id));
    }
}
