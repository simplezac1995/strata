// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.web.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import tools.jackson.databind.ObjectMapper;

/** Default writer: the same Problem Details envelope the staff API uses everywhere else. */
public class ProblemDetailErrorWriter implements IdempotencyErrorWriter {

    private final ObjectMapper objectMapper;

    public ProblemDetailErrorWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode, String message)
            throws IOException {
        ProblemDetail detail = ProblemDetails.of(errorCode, message);
        response.setStatus(detail.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), detail);
    }
}
