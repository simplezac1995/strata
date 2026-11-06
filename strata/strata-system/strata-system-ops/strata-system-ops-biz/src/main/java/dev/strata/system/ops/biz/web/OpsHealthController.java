// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.OpsHealthDto;
import dev.strata.system.ops.biz.service.OpsHealthAppService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ops/health")
public class OpsHealthController {

    private final OpsHealthAppService opsHealthAppService;

    public OpsHealthController(OpsHealthAppService opsHealthAppService) {
        this.opsHealthAppService = opsHealthAppService;
    }

    @GetMapping("/check")
    @PreAuthorize(Permissions.OPS_HEALTH_LIST)
    public Result<OpsHealthDto> check() {
        return Result.ok(opsHealthAppService.check());
    }
}
