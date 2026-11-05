// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.FundFlowDto;
import dev.strata.system.api.dto.ReconciliationDailyDto;
import dev.strata.system.api.dto.ReconciliationSummaryDto;
import dev.strata.system.api.request.RunReconciliationRequest;
import dev.strata.system.ops.biz.service.ReconciliationAppService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/reconciliation")
public class ReconciliationController {

    private final ReconciliationAppService reconciliationAppService;

    public ReconciliationController(ReconciliationAppService reconciliationAppService) {
        this.reconciliationAppService = reconciliationAppService;
    }

    @GetMapping("/daily")
    @PreAuthorize(Permissions.SYSTEM_RECONCILIATION_LIST)
    public Result<PageResult<ReconciliationDailyDto>> daily(
            @RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(reconciliationAppService.pageDaily(p, s));
    }

    @PostMapping("/run")
    @PreAuthorize(Permissions.SYSTEM_RECONCILIATION_CREATE)
    public Result<ReconciliationDailyDto> run(@Valid @RequestBody RunReconciliationRequest request) {
        return Result.ok(reconciliationAppService.run(request));
    }

    @GetMapping("/fund-flow")
    @PreAuthorize(Permissions.SYSTEM_RECONCILIATION_LIST)
    public Result<PageResult<FundFlowDto>> fundFlow(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(reconciliationAppService.pageFundFlow(userId, bizType, p, s));
    }

    @GetMapping("/summary")
    @PreAuthorize(Permissions.SYSTEM_RECONCILIATION_LIST)
    public Result<ReconciliationSummaryDto> summary() {
        return Result.ok(reconciliationAppService.summary());
    }
}
