// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.HeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Central browser security headers for every HTTP surface (staff API + SPA and the player {@code /v1}
 * API). Both filter chains call {@link #apply(HttpSecurity)} so the policy cannot drift between them.
 *
 * <p>The Content-Security-Policy is tuned for the React 19 + MUI 9 (Emotion) single-page app:
 * <ul>
 *   <li>{@code script-src 'self'} — the Vite build emits only externalised chunks, no inline script
 *       (verified against the built {@code index.html}), so inline script execution is refused;</li>
 *   <li>{@code style-src 'self' 'unsafe-inline'} — Emotion/MUI injects {@code <style>} at runtime, which
 *       requires inline styles; scripts stay locked down;</li>
 *   <li>{@code img/font/worker} allow {@code data:}/{@code blob:} for avatars, fonts and the BPMN modeler;</li>
 *   <li>{@code frame-ancestors 'none'} (with {@code X-Frame-Options: DENY}) prevents clickjacking on the
 *       admin/player surfaces.</li>
 * </ul>
 *
 * <p><b>Same-origin iframe exception.</b> The staff SPA embeds the backend Swagger UI in an iframe
 * (the {@code /tools/api-docs} page, {@code SWAGGER_UI_URL = /swagger-ui/index.html}). The global
 * {@code frame-ancestors 'none'} / {@code X-Frame-Options: DENY} would make the browser refuse to embed
 * it. {@link FrameAndCspWriter} therefore keeps DENY/{@code 'none'} for every business response but
 * relaxes only the framing directives to {@code SAMEORIGIN}/{@code frame-ancestors 'self'} for the
 * Swagger UI / OpenAPI surface. In the all-in-one jar image the SPA and the API are same-origin, so
 * same-origin framing is sufficient; in dev the Vite proxy forwards these same paths.
 *
 * <p>When the SPA and API are served from different origins, the reverse proxy serving the HTML must add
 * the same CSP with the API origin added to {@code connect-src}; the compose/Caddy template does this.
 * HSTS is emitted by Spring only on secure (HTTPS) requests, so local HTTP development is unaffected.
 */
public final class SecurityHeaders {

    /** Policy directives, also reused by the reverse-proxy templates so the edge and the app agree. */
    public static final String CONTENT_SECURITY_POLICY = String.join(
            "; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: blob:",
            "font-src 'self' data:",
            "connect-src 'self'",
            "worker-src 'self' blob:",
            "manifest-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    /**
     * Same policy but allowing same-origin framing, used only for the Swagger UI / OpenAPI surface that
     * the admin SPA embeds in an iframe. Every other directive is identical to {@link
     * #CONTENT_SECURITY_POLICY}.
     */
    public static final String CONTENT_SECURITY_POLICY_ALLOW_SAME_ORIGIN_FRAME =
            CONTENT_SECURITY_POLICY.replace("frame-ancestors 'none'", "frame-ancestors 'self'");

    /** Sensible minimum: disable powerful browser features the admin/player UIs never use. */
    public static final String PERMISSIONS_POLICY =
            "camera=(), microphone=(), geolocation=(), browsing-topics=(), payment=()";

    private SecurityHeaders() {}

    /**
     * Paths whose responses may be framed by the same-origin admin SPA. Only the Swagger UI static
     * bundle, the OpenAPI descriptors and its static assets are listed — business APIs, the player
     * chain and management endpoints stay unframeable.
     */
    private static final String[] FRAMEABLE_PREFIXES = {"/swagger-ui", "/v3/api-docs", "/swagger-resources", "/webjars"
    };

    /**
     * Path-aware writer that owns {@code X-Frame-Options} and the {@code frame-ancestors} directive of
     * the CSP. Defaults to the strict clickjacking lockdown; relaxes framing to same-origin only on the
     * {@link #FRAMEABLE_PREFIXES Swagger surface}. The built-in default frame-options writer is disabled
     * (see {@code frameOptions(disable())}) so this is the single authority for those two headers.
     */
    static final class FrameAndCspWriter implements HeaderWriter {

        @Override
        public void writeHeaders(HttpServletRequest request, HttpServletResponse response) {
            boolean frameable = isFrameable(request.getRequestURI());
            response.setHeader("X-Frame-Options", frameable ? "SAMEORIGIN" : "DENY");
            response.setHeader(
                    "Content-Security-Policy",
                    frameable ? CONTENT_SECURITY_POLICY_ALLOW_SAME_ORIGIN_FRAME : CONTENT_SECURITY_POLICY);
        }

        private static boolean isFrameable(String uri) {
            if (uri == null) return false;
            for (String prefix : FRAMEABLE_PREFIXES) {
                if (uri.startsWith(prefix)) return true;
            }
            return false;
        }
    }

    public static HttpSecurity apply(HttpSecurity http) throws Exception {
        http.headers(headers -> {
            // X-Frame-Options is a Spring Security default; disable it so the path-aware writer below is
            // the only authority. CSP is opt-in, so simply never register the default writer.
            headers.frameOptions(frame -> frame.disable());
            headers.addHeaderWriter(new FrameAndCspWriter());
            headers.referrerPolicy(referrer ->
                    referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
            headers.permissionsPolicyHeader(permissions -> permissions.policy(PERMISSIONS_POLICY));
            // One year, include subdomains. Deliberately no `preload` so a misissue can still be rolled back.
            headers.httpStrictTransportSecurity(
                    hsts -> hsts.maxAgeInSeconds(31_536_000).includeSubDomains(true));
            // Explicit nosniff (Spring already enables it; stated here so the intent is visible in review).
            headers.contentTypeOptions(options -> {});
        });
        return http;
    }
}
