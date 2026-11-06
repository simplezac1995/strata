// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.ops.biz.web;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingests browser Web Vitals reports from the admin UI ({@code /api/v1/metrics/frontend}).
 *
 * <p>The admin console reports CLS/INP/LCP/TTFB/CLS style metrics in production; without this endpoint the
 * reports silently 404. The endpoint is unauthenticated (like the CSP report) because it must work for pages that
 * load before the user logs in, and it only records a bounded set of metric/rating tag values in Micrometer.
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class FrontendMetricsController {

    private final MeterRegistry registry;

    public FrontendMetricsController(MeterRegistry registry) {
        this.registry = registry;
    }

    @PostMapping("/frontend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void report(@RequestBody(required = false) Map<String, Object> payload) {
        String metric = sanitize(payload == null ? null : payload.get("name"));
        String rating = sanitize(payload == null ? null : payload.get("rating"));
        Counter.builder("strata.frontend.web.vitals")
                .description("Browser Web Vitals reported by the admin UI")
                .tag("metric", metric)
                .tag("rating", rating)
                .register(registry)
                .increment();
    }

    /** Keeps the tag cardinality bounded: only lowercase names/ratings, everything else folds into {@code other}. */
    private static String sanitize(Object value) {
        if (value == null) {
            return "unknown";
        }
        String text = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return "unknown";
        }
        String safe = text.replaceAll("[^a-z0-9._-]", "_");
        return safe.length() > 32 ? "other" : safe;
    }
}
