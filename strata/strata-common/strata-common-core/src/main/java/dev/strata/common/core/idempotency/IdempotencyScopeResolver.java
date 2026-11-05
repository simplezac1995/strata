// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * Decides which bucket an {@code Idempotency-Key} belongs to. Keys are only unique per caller: without a
 * scope, one client's key would let another client's retry replay a response that is not theirs.
 */
@FunctionalInterface
public interface IdempotencyScopeResolver {

    String resolveScope(HttpServletRequest request);

    /**
     * Default: the authenticated principal when there is one (staff JWT or player session both surface
     * through the servlet API), otherwise the client address. Deliberately dependency-free — the core
     * module must not require Spring Security to be on the classpath.
     */
    static IdempotencyScopeResolver principalOrRemoteAddress() {
        return request -> {
            if (request.getUserPrincipal() != null && request.getUserPrincipal().getName() != null) {
                return fit("user:" + request.getUserPrincipal().getName());
            }
            return fit("addr:" + Objects.toString(request.getRemoteAddr(), "unknown"));
        };
    }

    /** The scope column is VARCHAR(64); truncating keeps an oversized principal from failing an insert. */
    private static String fit(String scope) {
        return scope.length() <= 64 ? scope : scope.substring(0, 64);
    }
}
