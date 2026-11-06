// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.web;

import dev.strata.common.core.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Rejects an oversize {@code application/json} request body with 413 before it is read.
 *
 * <p>The embedded container's {@code max-http-form-post-size} only governs form bodies; a JSON POST had no ceiling
 * until this filter. It inspects the declared {@code Content-Length}: when the Content-Type is JSON and the declared
 * length exceeds the configured limit, the request is refused with a Problem Details 413 and never reaches the message
 * converter. Bodies without a declared length (chunked transfer) are not buffered here — that would buffer every
 * request —; the container's {@code max-swallow-size} is the backstop for those.
 */
public class JsonBodyLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JsonBodyLimitFilter.class);

    private final WebHardeningProperties properties;
    private final ObjectMapper objectMapper;

    public JsonBodyLimitFilter(WebHardeningProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.jsonBodyLimitEnabled()) {
            chain.doFilter(request, response);
            return;
        }
        String contentType = request.getContentType();
        long declared = request.getContentLengthLong();
        boolean json = contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith(MediaType.APPLICATION_JSON_VALUE);
        if (json && declared > properties.jsonMaxBytes()) {
            log.warn(
                    "Rejecting {} {} : declared JSON body of {} byte(s) exceeds limit of {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    declared,
                    properties.jsonMaxBytes());
            ProblemDetail body = ProblemDetails.of(
                    ErrorCode.PAYLOAD_TOO_LARGE,
                    "Request body exceeds the " + properties.jsonMaxBytes() + " byte JSON limit");
            response.setStatus(body.getStatus());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }
        chain.doFilter(request, response);
    }
}
