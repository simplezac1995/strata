// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.PageResult;
import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.SignInConfigDto;
import dev.strata.system.api.dto.SignInRecordDto;
import dev.strata.system.api.request.SignInConfigUpsertRequest;
import dev.strata.system.ops.biz.service.SignInAppService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/ops/signin")
public class SignInController {

    private final SignInAppService signInAppService;

    public SignInController(SignInAppService signInAppService) {
        this.signInAppService = signInAppService;
    }

    @GetMapping("/config")
    @PreAuthorize(Permissions.OPS_SIGNIN_LIST)
    public Result<List<SignInConfigDto>> config() {
        return Result.ok(signInAppService.getConfig());
    }

    @PutMapping("/config")
    @PreAuthorize(Permissions.OPS_SIGNIN_UPDATE)
    public Result<List<SignInConfigDto>> replaceConfig(@Valid @RequestBody SignInConfigUpsertRequest request) {
        return Result.ok(signInAppService.replaceConfig(request));
    }

    @GetMapping("/records")
    @PreAuthorize(Permissions.OPS_SIGNIN_LIST)
    public Result<PageResult<SignInRecordDto>> records(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(signInAppService.records(userId, page, size));
    }

    /** Current player signs in. TODO: real reward grant (module stub). */
    @PostMapping("/checkin")
    public Result<SignInRecordDto> checkin() {
        return Result.ok(signInAppService.checkin());
    }
}
