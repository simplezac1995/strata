// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives browser Content-Security-Policy violation reports (P2-11).
 *
 * <p>This is a firehose, not a store: it counts the report and logs it, then returns 204. Browsers post these without
 * any credential, so the path is public in the security chain; the body is parsed leniently so a malformed report can
 * never crash the endpoint. A real collector ships the reports off-box; the alert signal is the
 * {@code csp_report_total} counter.
 */
@RestController
@RequestMapping("/api/v1/system/security")
public class CspReportController {

    private static final Logger log = LoggerFactory.getLogger(CspReportController.class);

    private final SecurityMetrics metrics;

    public CspReportController(SecurityMetrics metrics) {
        this.metrics = metrics;
    }

    @PostMapping(
            path = "/csp-report",
            consumes = {"application/csp-report", "application/reports+json", "application/json"})
    public ResponseEntity<Void> report(@RequestBody(required = false) String body) {
        metrics.cspReport();
        if (body != null && !body.isBlank()) {
            // Browsers can POST large reports; keep the log line bounded.
            log.warn("CSP violation report: {}", body.length() > 2000 ? body.substring(0, 2000) : body);
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
