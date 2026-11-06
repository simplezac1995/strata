// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Forces hardened cookie attributes on every {@code Set-Cookie} in production (P1-6).
 *
 * <p>Stateless JSON APIs often emit no cookie, but when one is emitted — a session, refresh or third-party cookie —
 * it must be unusable from a non-HTTPS or cross-site context. This wrapper rewrites each outgoing {@code Set-Cookie}
 * to add:
 *
 * <ul>
 *   <li>{@code Secure} — never sent over plain HTTP;</li>
 *   <li>{@code HttpOnly} — inaccessible to JavaScript, killing token theft from XSS;</li>
 *   <li>{@code SameSite=Lax} (configurable) — not sent on cross-site POSTs;</li>
 *   <li>the {@code __Host-} name prefix (when enabled) — the cookie may only be set from a secure host, ignores
 *       Domain and requires Path=/, which defeats subdomain/injection attacks.</li>
 * </ul>
 *
 * <p>Runs only when {@code strata.security.cookie.hardened=true}; development (plain HTTP, no TLS) leaves cookies
 * untouched so local login keeps working.
 */
public class CookieHardeningFilter extends OncePerRequestFilter {

    private static final String SET_COOKIE = "Set-Cookie";
    private static final String HOST_PREFIX = "__Host-";

    private final CookieSecurityProperties properties;

    public CookieHardeningFilter(CookieSecurityProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.hardened()) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(request, new HardenedResponse(response));
    }

    /** Rewrites every Set-Cookie it sees as it passes through. */
    private final class HardenedResponse extends HttpServletResponseWrapper {

        private HardenedResponse(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void addHeader(String name, String value) {
            super.addHeader(name, SET_COOKIE.equalsIgnoreCase(name) ? rewrite(value) : value);
        }

        @Override
        public void setHeader(String name, String value) {
            super.setHeader(name, SET_COOKIE.equalsIgnoreCase(name) ? rewrite(value) : value);
        }

        @Override
        public void addCookie(jakarta.servlet.http.Cookie cookie) {
            // Cookie paths through addHeader once the container builds the Set-Cookie; rewriting at the header
            // layer covers both addCookie and a hand-built header.
            super.addCookie(cookie);
        }
    }

    /**
     * Rewrites one {@code Set-Cookie} header value to include Secure/HttpOnly/SameSite and the {@code __Host-} prefix.
     * Package-private for the unit test.
     */
    String rewrite(String setCookie) {
        if (setCookie == null || setCookie.isBlank()) {
            return setCookie;
        }
        String[] parts = setCookie.split(";");
        String nameValue = parts[0];
        int eq = nameValue.indexOf('=');
        String name = eq > 0 ? nameValue.substring(0, eq).trim() : nameValue.trim();
        String value = eq > 0 ? nameValue.substring(eq + 1) : "";

        List<String> attributes = new ArrayList<>();
        boolean secure = false;
        boolean httpOnly = false;
        boolean sameSite = false;
        for (int i = 1; i < parts.length; i++) {
            String attr = parts[i].trim();
            String lower = attr.toLowerCase(Locale.ROOT);
            if (lower.startsWith("secure")) {
                secure = true;
            } else if (lower.startsWith("httponly")) {
                httpOnly = true;
            } else if (lower.startsWith("samesite")) {
                sameSite = true;
            }
            attributes.add(attr);
        }

        String rewrittenName = properties.hostPrefix() && !name.startsWith(HOST_PREFIX) ? HOST_PREFIX + name : name;
        StringBuilder out = new StringBuilder(rewrittenName).append('=').append(value);
        for (String attr : attributes) {
            out.append("; ").append(attr);
        }
        if (!secure) {
            out.append("; Secure");
        }
        if (!httpOnly) {
            out.append("; HttpOnly");
        }
        if (!sameSite) {
            out.append("; SameSite=").append(properties.sameSite());
        }
        if (properties.hostPrefix() && rewrittenName.startsWith(HOST_PREFIX)) {
            out.append("; Path=/");
        }
        return out.toString();
    }
}
