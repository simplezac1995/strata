// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web.compliance;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.compliance.KycApplicationDto;
import dev.strata.system.api.request.compliance.KycReviewRequest;
import dev.strata.system.ops.biz.service.compliance.KycAppService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin KYC review queue. */
@Validated
@RestController
@RequestMapping("/api/v1/admin/kyc")
public class KycAdminController {

    private final KycAppService kycAppService;

    public KycAdminController(KycAppService kycAppService) {
        this.kycAppService = kycAppService;
    }

    @GetMapping("/applications")
    @PreAuthorize(Permissions.COMPLIANCE_KYC_LIST)
    public Result<PageResult<KycApplicationDto>> page(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int p = page == null ? 0 : page;
        int s = size == null ? 20 : size;
        return Result.ok(kycAppService.page(status, p, s));
    }

    @PutMapping("/applications/{id}/review")
    @PreAuthorize(Permissions.COMPLIANCE_KYC_UPDATE)
    public Result<KycApplicationDto> review(
            @PathVariable Long id, Authentication authentication, @Valid @RequestBody KycReviewRequest request) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(kycAppService.review(id, principal.userId(), request));
    }
}
