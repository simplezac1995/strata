// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.RiskRuleDto;
import dev.strata.system.api.request.RiskRuleCreateRequest;
import dev.strata.system.api.request.RiskRuleUpdateRequest;
import dev.strata.system.ops.biz.service.RiskRuleAppService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/risk/rules")
public class RiskRuleController {

    private final RiskRuleAppService riskRuleAppService;

    public RiskRuleController(RiskRuleAppService riskRuleAppService) {
        this.riskRuleAppService = riskRuleAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.SYSTEM_RISK_LIST)
    public Result<PageResult<RiskRuleDto>> page(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(riskRuleAppService.page(type, enabled, p, s));
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_RISK_LIST)
    public Result<RiskRuleDto> get(@PathVariable Long id) {
        return Result.ok(riskRuleAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.SYSTEM_RISK_CREATE)
    public Result<RiskRuleDto> create(@Valid @RequestBody RiskRuleCreateRequest request) {
        return Result.ok(riskRuleAppService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_RISK_UPDATE)
    public Result<RiskRuleDto> update(@PathVariable Long id, @Valid @RequestBody RiskRuleUpdateRequest request) {
        return Result.ok(riskRuleAppService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(Permissions.SYSTEM_RISK_DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        riskRuleAppService.delete(id);
        return Result.ok();
    }
}
