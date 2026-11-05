// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.ratelimit;

import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/** Default writer: the Problem Details envelope the staff API uses, with the retry hint callers act on. */
public class ProblemDetailRateLimitWriter implements RateLimitErrorWriter {

    private final ObjectMapper objectMapper;

    public ProblemDetailRateLimitWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(
            HttpServletRequest request,
            HttpServletResponse response,
            String ruleName,
            long retryAfterSeconds,
            String message)
            throws IOException {
        ProblemDetail detail = ProblemDetails.of(ErrorCode.TOO_MANY_REQUESTS, message);
        response.setStatus(ErrorCode.TOO_MANY_REQUESTS.status().value());
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), detail);
    }
}
