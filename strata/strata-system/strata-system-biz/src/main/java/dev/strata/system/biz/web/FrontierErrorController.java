// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.web;

import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.request.FrontierErrorRequest;
import dev.strata.system.biz.frontier.FrontierErrorAppService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Browser-side error-report receiver (#21). Lives inside the JWT chain (the admin axios instance attaches
 * the token) so no security whitelist change is needed. Always answers 202 Accepted — whether the report
 * was logged or silently rate-limited — so a flooded client never gets a retry signal.
 */
@RestController
@RequestMapping("/api/v1/system")
public class FrontierErrorController {

    private final FrontierErrorAppService frontierErrorAppService;

    public FrontierErrorController(FrontierErrorAppService frontierErrorAppService) {
        this.frontierErrorAppService = frontierErrorAppService;
    }

    @PostMapping("/frontier-error")
    @PreAuthorize(Permissions.SYSTEM_FRONTIERERROR_CREATE)
    public ResponseEntity<Void> report(
            @RequestBody(required = false) FrontierErrorRequest request, HttpServletRequest servletRequest) {
        frontierErrorAppService.report(request, clientIp(servletRequest));
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
            return first.trim();
        }
        return request.getRemoteAddr();
    }
}
