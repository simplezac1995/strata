// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.web;

import dev.strata.common.core.exception.ErrorCode;
import java.net.URI;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;

/**
 * Centralized Problem Details factory so {@code title}/{@code type}/{@code code}/{@code timestamp}/{@code traceId} are
 * assembled identically everywhere (controller advice + security handlers).
 */
public final class ProblemDetails {

    private static final String ERROR_TYPE_BASE = "https://strata.dev/errors/";

    private ProblemDetails() {}

    public static ProblemDetail of(ErrorCode errorCode, String detailText) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(errorCode.status(), detailText);
        detail.setTitle(errorCode.name());
        detail.setType(URI.create(ERROR_TYPE_BASE + errorCode.name().toLowerCase()));
        detail.setProperty("code", errorCode.code());
        detail.setProperty("timestamp", Instant.now().toString());
        String traceId = MDC.get(TraceIdFilter.MDC_KEY);
        if (traceId != null) {
            detail.setProperty("traceId", traceId);
        }
        return detail;
    }
}
