// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.api.request;

import java.time.Instant;
import java.util.Map;

/**
 * Frontend error-report body (#21), as posted by the browser {@code errorReporter} (axios or
 * {@code sendBeacon}). Received and logged; never persisted.
 */
public record FrontierErrorRequest(
        String message,
        String stack,
        String url,
        String userId,
        String userAgent,
        String level,
        Instant timestamp,
        Map<String, Object> context) {}
