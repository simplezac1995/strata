// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * P2-11: the security counters increment as expected, and the CSP report receiver only records + returns 204.
 */
class SecurityMetricsTest {

    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final SecurityMetrics metrics = new SecurityMetrics(registry);

    @Test
    void loginFailureCounter() {
        metrics.loginFailure();
        metrics.loginFailure();

        assertThat(registry.find("login_failure_total").counter().count()).isEqualTo(2.0);
    }

    @Test
    void rejectionCounterTaggedByStatus() {
        metrics.securityRejection(403);
        metrics.securityRejection(403);
        metrics.securityRejection(415);

        assertThat(registry.find("security_rejection_total")
                        .tag("status", "403")
                        .counter()
                        .count())
                .isEqualTo(2.0);
        assertThat(registry.find("security_rejection_total")
                        .tag("status", "415")
                        .counter()
                        .count())
                .isEqualTo(1.0);
    }

    @Test
    void cspReportCounter() {
        metrics.cspReport();

        assertThat(registry.find("csp_report_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void cspReportEndpointReturns204AndCounts() {
        CspReportController controller = new CspReportController(metrics);

        ResponseEntity<Void> response = controller.report("{\"csp-report\":{}}");

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(registry.find("csp_report_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void cspReportEmptyBodyStill204() {
        CspReportController controller = new CspReportController(metrics);

        assertThat(controller.report(null).getStatusCode().value()).isEqualTo(204);
    }
}
