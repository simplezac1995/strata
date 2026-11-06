// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web.compliance;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.compliance.AmlReportDto;
import dev.strata.system.api.dto.compliance.AmlRuleDto;
import dev.strata.system.api.request.compliance.AmlReportReviewRequest;
import dev.strata.system.api.request.compliance.AmlRuleCreateRequest;
import dev.strata.system.api.request.compliance.AmlRuleUpdateRequest;
import dev.strata.system.ops.biz.service.compliance.AmlAppService;
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

/** Admin AML rules and report review queue. */
@Validated
@RestController
@RequestMapping("/api/v1/admin/aml")
public class AmlAdminController {

    private final AmlAppService amlAppService;

    public AmlAdminController(AmlAppService amlAppService) {
        this.amlAppService = amlAppService;
    }

    @GetMapping("/rules")
    @PreAuthorize(Permissions.COMPLIANCE_AML_LIST)
    public Result<PageResult<AmlRuleDto>> pageRules(
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(amlAppService.pageRules(ruleType, enabled, p, s));
    }

    @PostMapping("/rules")
    @PreAuthorize(Permissions.COMPLIANCE_AML_CREATE)
    public Result<AmlRuleDto> createRule(@Valid @RequestBody AmlRuleCreateRequest request) {
        return Result.ok(amlAppService.createRule(request));
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize(Permissions.COMPLIANCE_AML_UPDATE)
    public Result<AmlRuleDto> updateRule(@PathVariable Long id, @Valid @RequestBody AmlRuleUpdateRequest request) {
        return Result.ok(amlAppService.updateRule(id, request));
    }

    @DeleteMapping("/rules/{id}")
    @PreAuthorize(Permissions.COMPLIANCE_AML_DELETE)
    public Result<Void> deleteRule(@PathVariable Long id) {
        amlAppService.deleteRule(id);
        return Result.ok();
    }

    @GetMapping("/reports")
    @PreAuthorize(Permissions.COMPLIANCE_AML_LIST)
    public Result<PageResult<AmlReportDto>> pageReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(amlAppService.pageReports(status, p, s));
    }

    @PutMapping("/reports/{id}/review")
    @PreAuthorize(Permissions.COMPLIANCE_AML_UPDATE)
    public Result<AmlReportDto> reviewReport(
            @PathVariable Long id, @Valid @RequestBody AmlReportReviewRequest request) {
        return Result.ok(amlAppService.reviewReport(id, request));
    }
}
