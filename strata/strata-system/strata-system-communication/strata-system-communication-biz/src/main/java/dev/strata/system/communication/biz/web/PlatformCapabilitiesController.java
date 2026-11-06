// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.communication.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.system.api.dto.PlatformCapabilitiesDto;
import dev.strata.system.communication.biz.service.PlatformCapabilitiesAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system/capabilities")
public class PlatformCapabilitiesController {

    private final PlatformCapabilitiesAppService platformCapabilitiesAppService;

    public PlatformCapabilitiesController(PlatformCapabilitiesAppService platformCapabilitiesAppService) {
        this.platformCapabilitiesAppService = platformCapabilitiesAppService;
    }

    @GetMapping
    public Result<PlatformCapabilitiesDto> capabilities() {
        return Result.ok(platformCapabilitiesAppService.capabilities());
    }
}
