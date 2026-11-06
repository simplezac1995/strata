// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web.compliance;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.compliance.KycApplicationDto;
import dev.strata.system.api.request.compliance.KycSubmitRequest;
import dev.strata.system.ops.biz.service.compliance.KycAppService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Player-facing KYC endpoints (self service, no admin authority needed). */
@Validated
@RestController
@RequestMapping("/api/v1/kyc")
public class KycController {

    private final KycAppService kycAppService;

    public KycController(KycAppService kycAppService) {
        this.kycAppService = kycAppService;
    }

    @PostMapping("/submit")
    public Result<KycApplicationDto> submit(
            Authentication authentication, @Valid @RequestBody KycSubmitRequest request) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(kycAppService.submit(principal.userId(), request));
    }

    @GetMapping("/my-status")
    public Result<KycApplicationDto> myStatus(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(kycAppService.myStatus(principal.userId()));
    }
}
