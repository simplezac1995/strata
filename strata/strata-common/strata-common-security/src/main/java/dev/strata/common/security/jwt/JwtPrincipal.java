// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.jwt;

import java.security.Principal;

/**
 * The authenticated staff principal.
 *
 * <p>It implements {@link Principal} so it answers {@code HttpServletRequest#getUserPrincipal()} with a
 * stable name. Without that, the servlet API falls back to the authentication token's own name — which for
 * a plain record is {@code toString()}, i.e. a string that changes whenever a field is added. Anything that
 * keys on the caller's identity (the generic idempotency scope, for instance) needs a name that does not
 * move.
 */
public record JwtPrincipal(Long userId, String username) implements Principal {

    @Override
    public String getName() {
        return username;
    }
}
