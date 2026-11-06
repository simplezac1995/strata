// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.RiskIpBlacklistDto;
import dev.strata.system.api.request.RiskIpBlacklistCreateRequest;
import dev.strata.system.ops.biz.service.RiskIpBlacklistAppService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/risk/ip-blacklist")
public class RiskIpBlacklistController {

    private final RiskIpBlacklistAppService riskIpBlacklistAppService;

    public RiskIpBlacklistController(RiskIpBlacklistAppService riskIpBlacklistAppService) {
        this.riskIpBlacklistAppService = riskIpBlacklistAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_RISK_LIST)
    public Result<PageResult<RiskIpBlacklistDto>> page(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(riskIpBlacklistAppService.page(p, s));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_RISK_CREATE)
    public Result<RiskIpBlacklistDto> create(@Valid @RequestBody RiskIpBlacklistCreateRequest request) {
        return Result.ok(riskIpBlacklistAppService.create(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_RISK_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        riskIpBlacklistAppService.delete(id);
        return Result.ok();
    }
}
