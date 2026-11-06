// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.web;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Micrometer counters for security-relevant events, exposed on the management port for alerting (P2-11).
 *
 * <p>These are signal counters only — they do not call a webhook on their own; an alert rule in the metrics stack
 * pages someone when a counter's rate climbs. Kept deliberately tiny and dependency-free so any layer (filter,
 * login service, CSP receiver) can record without knowing how metrics are scraped.
 *
 * <ul>
 *   <li>{@code login_failure_total} — a failed authentication attempt.</li>
 *   <li>{@code security_rejection_total{status}} — a request refused by a server-side guard (403 origin, 415
 *       wrong content-type, 429 throttled).</li>
 *   <li>{@code csp_report_total} — a browser CSP violation report received.</li>
 * </ul>
 */
public class SecurityMetrics {

    private static final Logger log = LoggerFactory.getLogger(SecurityMetrics.class);

    private final MeterRegistry registry;

    public SecurityMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** A failed login (bad credentials, locked account, MFA failure). */
    public void loginFailure() {
        registry.counter("login_failure_total").increment();
    }

    /** A request refused by a guard filter, tagged with the HTTP status (403/415/429). */
    public void securityRejection(int status) {
        registry.counter("security_rejection_total", Tags.of("status", String.valueOf(status)))
                .increment();
    }

    /** A browser posted a Content-Security-Policy violation report. */
    public void cspReport() {
        registry.counter("csp_report_total").increment();
        log.debug("CSP violation report received");
    }
}
