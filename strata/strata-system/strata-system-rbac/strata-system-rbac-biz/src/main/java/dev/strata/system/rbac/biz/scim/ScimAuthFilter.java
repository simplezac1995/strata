// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.scim;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Guards the SCIM endpoints with the configured bearer token. The endpoints are also permit-listed in the security
 * chain (they are machine-to-machine, not JWT-based); this filter is the only authentication gate, and it fails closed
 * when {@code strata.scim.token} is empty.
 */
@Component
@ConditionalOnProperty(prefix = "strata.scim", name = "enabled", havingValue = "true")
public class ScimAuthFilter extends OncePerRequestFilter {

    private static final String PREFIX = "/scim/v2/";

    private final ScimProperties properties;

    public ScimAuthFilter(ScimProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith(PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String expected = properties.getToken();
        String header = request.getHeader("Authorization");
        String provided = header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
        boolean ok = !expected.isBlank()
                && MessageDigest.isEqual(
                        expected.getBytes(StandardCharsets.UTF_8), provided.getBytes(StandardCharsets.UTF_8));
        if (!ok) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/scim+json");
            response.getWriter()
                    .write("{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:Error\"],\"status\":\"401\","
                            + "\"detail\":\"Invalid or missing SCIM bearer token\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
