// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.ratelimit.RateLimit;
import dev.strata.system.api.dto.TokenResponse;
import dev.strata.system.api.request.SamlExchangeRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * SAML 2.0 service-provider endpoints: login-page config, the SP-initiated redirect, the POST binding ACS and the
 * SPA's one-time code exchange.
 */
@Validated
@RestController
@ConditionalOnProperty(prefix = "strata.saml", name = "enabled", havingValue = "true")
@RequestMapping
public class SamlController {

    private final SamlProperties properties;
    private final SamlRequestService requestService;
    private final SamlLoginService loginService;

    public SamlController(SamlProperties properties, SamlRequestService requestService, SamlLoginService loginService) {
        this.properties = properties;
        this.requestService = requestService;
        this.loginService = loginService;
    }

    @RateLimit(limit = 20, window = 60, key = "saml_authorize")
    @GetMapping("/saml2/authenticate")
    public ResponseEntity<Void> authenticate() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(requestService.beginUrl()))
                .build();
    }

    @RateLimit(limit = 20, window = 60, key = "saml_acs")
    @PostMapping("/saml2/acs")
    public ResponseEntity<Void> acs(
            @RequestParam("SAMLResponse") String samlResponse,
            @RequestParam(value = "RelayState", required = false) String relayState,
            HttpServletRequest request) {
        String redirect = loginService.complete(
                samlResponse, relayState, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirect))
                .build();
    }

    @RateLimit(limit = 20, window = 60, key = "saml_exchange")
    @PostMapping("/api/v1/system/auth/saml/exchange")
    public Result<TokenResponse> exchange(@Valid @RequestBody SamlExchangeRequest request) {
        return Result.ok(loginService.exchange(request.code()));
    }
}
