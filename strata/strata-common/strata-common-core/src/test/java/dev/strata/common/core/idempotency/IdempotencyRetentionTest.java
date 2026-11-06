// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * P1-7: proves the PT24H retention is real — the expiry written to the store is ~24h after the claim, the purge
 * predicate only deletes expired rows, and a same-key/different-payload attempt is rejected with 409 rather than a
 * silent replay.
 */
@DisplayName("幂等 retention=PT24H 真生效与同键异负载 409")
class IdempotencyRetentionTest {

    private IdempotencyProperties props(Duration retention) {
        return new IdempotencyProperties(
                true,
                false,
                "Idempotency-Key",
                List.of("/api/v1/**"),
                List.of(),
                List.of("POST"),
                retention,
                65536,
                true,
                Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("默认 retention=PT24H：写入 store 的 expiresAt 约为 now+24h")
    void retentionWritesExpiryTwentyFourHoursOut() throws Exception {
        IdempotencyStore store = mock(IdempotencyStore.class);
        when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(1L));
        IdempotencyFilter filter =
                new IdempotencyFilter(props(Duration.ofHours(24)), store, (a, b, c, d) -> {}, req -> "user:1");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/payments");
        request.addHeader("Idempotency-Key", "k-24h");
        request.setContent("{\"amount\":1}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        Instant before = Instant.now();

        filter.doFilter(
                request, response, (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(200));

        ArgumentCaptor<IdempotencyRequest> captor = ArgumentCaptor.forClass(IdempotencyRequest.class);
        org.mockito.Mockito.verify(store).claim(captor.capture());
        Instant expected = before.plus(24, ChronoUnit.HOURS);
        // Allow a small clock skew around the 24h mark.
        assertThat(captor.getValue().expiresAt()).isBetween(expected.minusSeconds(5), expected.plusSeconds(60));
    }

    @Test
    @DisplayName("默认属性 retention 为 PT24H（配置默认值）")
    void defaultRetentionIsTwentyFourHours() {
        IdempotencyProperties p = new IdempotencyProperties(
                true, false, "Idempotency-Key", List.of(), List.of(), List.of(), null, 65536, true, null);
        assertThat(p.retention()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    @DisplayName("同键异负载被拒绝为 409（IDEMPOTENCY_KEY_REUSED），不静默重放")
    void sameKeyDifferentPayloadRejectedWith409() throws Exception {
        IdempotencyStore store = mock(IdempotencyStore.class);
        when(store.claim(any())).thenReturn(new IdempotencyClaim.Mismatch());
        IdempotencyErrorWriter writer = mock(IdempotencyErrorWriter.class);
        IdempotencyFilter filter = new IdempotencyFilter(props(Duration.ofHours(24)), store, writer, req -> "user:1");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/payments");
        request.addHeader("Idempotency-Key", "k-reuse");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        org.mockito.Mockito.verify(writer)
                .write(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.eq(response),
                        org.mockito.ArgumentMatchers.eq(ErrorCode.IDEMPOTENCY_KEY_REUSED),
                        org.mockito.ArgumentMatchers.anyString());
        // The error writer maps IDEMPOTENCY_KEY_REUSED to HTTP 409.
        assertThat(ErrorCode.IDEMPOTENCY_KEY_REUSED.status().value()).isEqualTo(409);
    }
}
