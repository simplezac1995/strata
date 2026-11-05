// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web.compliance;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.jwt.JwtPrincipal;
import dev.strata.system.api.dto.compliance.CoolOffDto;
import dev.strata.system.api.dto.compliance.ResponsibleGamblingLimitDto;
import dev.strata.system.api.dto.compliance.ResponsibleLimitsView;
import dev.strata.system.api.dto.compliance.SelfExclusionDto;
import dev.strata.system.api.request.compliance.CoolOffCreateRequest;
import dev.strata.system.api.request.compliance.ResponsibleLimitCreateRequest;
import dev.strata.system.api.request.compliance.SelfExclusionCreateRequest;
import dev.strata.system.ops.biz.service.compliance.ResponsibleGamblingAppService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Player self-service for responsible-gambling tools; admin oversight lives under /admin. */
@Validated
@RestController
@RequestMapping("/api/v1/player/responsible")
public class ResponsibleGamblingController {

    private final ResponsibleGamblingAppService responsibleGamblingAppService;

    public ResponsibleGamblingController(ResponsibleGamblingAppService responsibleGamblingAppService) {
        this.responsibleGamblingAppService = responsibleGamblingAppService;
    }

    @GetMapping("/my-limits")
    public Result<ResponsibleLimitsView> myLimits(Authentication authentication) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(responsibleGamblingAppService.myLimits(principal.userId()));
    }

    @PostMapping("/limits")
    public Result<ResponsibleGamblingLimitDto> createLimit(
            Authentication authentication, @Valid @RequestBody ResponsibleLimitCreateRequest request) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(responsibleGamblingAppService.createLimit(principal.userId(), request));
    }

    @PostMapping("/self-exclusion")
    public Result<SelfExclusionDto> selfExclusion(
            Authentication authentication, @Valid @RequestBody SelfExclusionCreateRequest request) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(responsibleGamblingAppService.selfExclude(principal.userId(), request));
    }

    @PostMapping("/cool-off")
    public Result<CoolOffDto> coolOff(Authentication authentication, @Valid @RequestBody CoolOffCreateRequest request) {
        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        return Result.ok(responsibleGamblingAppService.coolOff(principal.userId(), request));
    }
}
