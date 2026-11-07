// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml;

import dev.strata.common.core.web.Result;
import dev.strata.system.api.dto.SamlConfigResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Always-on login-page config: tells the SPA whether to show a SAML button. */
@RestController
@RequestMapping("/api/v1/system/auth/saml")
public class SamlConfigController {

    private final SamlProperties properties;

    public SamlConfigController(SamlProperties properties) {
        this.properties = properties;
    }

    @GetMapping("/config")
    public Result<SamlConfigResponse> config() {
        return Result.ok(new SamlConfigResponse(properties.isEnabled(), properties.getDisplayName()));
    }
}
