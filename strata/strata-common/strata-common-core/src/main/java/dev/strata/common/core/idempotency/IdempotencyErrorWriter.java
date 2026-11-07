// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import dev.strata.common.core.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Writes an idempotency rejection. A filter runs outside the controller advice, so it has to render the
 * body itself.
 *
 * <p>Pluggable because the two surfaces do not share an envelope: the staff API answers with Problem
 * Details, while the player API owes the client {@code {code, message, requestId}} (ADR-0026,
 * docs/casino-integration.md §5). The player module can replace this bean instead of the framework
 * guessing which shape a given path wants.
 */
@FunctionalInterface
public interface IdempotencyErrorWriter {

    void write(HttpServletRequest request, HttpServletResponse response, ErrorCode errorCode, String message)
            throws IOException;
}
