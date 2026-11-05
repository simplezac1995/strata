// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * How a throttled request is answered.
 *
 * <p>An interface for the same reason the idempotency filter has one: this framework module cannot know which
 * surface it is protecting. A player endpoint must answer in the player's envelope (the client reads
 * {@code code}/{@code message}/{@code requestId}), while the staff API answers in Problem Details.
 */
@FunctionalInterface
public interface RateLimitErrorWriter {

    /**
     * Writes the refusal. The implementation sets the status, the {@code Retry-After} header and the body.
     *
     * @param retryAfterSeconds what the client is told to wait; never zero, because that invites an instant retry
     */
    void write(
            HttpServletRequest request,
            HttpServletResponse response,
            String ruleName,
            long retryAfterSeconds,
            String message)
            throws IOException;
}
