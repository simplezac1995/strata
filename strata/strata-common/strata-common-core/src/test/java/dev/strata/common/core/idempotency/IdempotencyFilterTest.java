// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Unit tests for {@link IdempotencyFilter}: every claim outcome, the key-required / too-long rejection
 * paths, the body-buffering cap, and the response replayability rules — all with the servlet request and
 * the store boundary mocked, no Spring context.
 */
class IdempotencyFilterTest {

    private static final String PATH = "/api/v1/payments";

    @Mock
    private IdempotencyStore store;

    @Mock
    private IdempotencyScopeResolver scopeResolver;

    private AutoCloseable mocks;
    private List<String[]> errors;
    private IdempotencyFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private IdempotencyProperties props(int maxBodyBytes) {
        return new IdempotencyProperties(
                true,
                false,
                "Idempotency-Key",
                List.of("/api/v1/**"),
                List.of("/api/v1/payments/**"),
                List.of("POST", "PUT", "PATCH", "DELETE"),
                Duration.ofHours(24),
                maxBodyBytes,
                true,
                Duration.ofMinutes(30));
    }

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        errors = new ArrayList<>();
        IdempotencyErrorWriter writer = (req, res, code, msg) -> errors.add(new String[] {code.name(), msg});
        when(scopeResolver.resolveScope(any())).thenReturn("user:1");
        request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI(PATH);
        response = new MockHttpServletResponse();
        buildFilter(65536, writer);
    }

    private void buildFilter(int maxBodyBytes, IdempotencyErrorWriter writer) {
        filter = new IdempotencyFilter(props(maxBodyBytes), store, writer, scopeResolver);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    private void withBody(String body) {
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
    }

    /** A downstream chain that reads the (cached) body and then renders a configurable response. */
    private FilterChain downstream(int status, String body, List<byte[]> capturedBodies) {
        return (req, res) -> {
            jakarta.servlet.http.HttpServletResponse httpRes = (jakarta.servlet.http.HttpServletResponse) res;
            byte[] read = req.getInputStream().readAllBytes();
            if (capturedBodies != null) {
                capturedBodies.add(read);
            }
            httpRes.setStatus(status);
            if (body != null) {
                httpRes.getWriter().write(body);
            }
        };
    }

    @Nested
    class Skipping {

        @Test
        void disabledSkips() throws Exception {
            IdempotencyProperties off = props(65536);
            filter = new IdempotencyFilter(
                    new IdempotencyProperties(
                            false,
                            false,
                            "Idempotency-Key",
                            List.of("/api/v1/**"),
                            List.of(),
                            List.of("POST"),
                            Duration.ofHours(1),
                            65536,
                            true,
                            Duration.ofMinutes(1)),
                    store,
                    (a, b, c, d) -> {},
                    scopeResolver);

            filter.doFilter(request, response, downstream(200, "ok", null));

            verify(store, never()).claim(any());
        }

        @Test
        void nonWriteMethodSkips() throws Exception {
            request.setMethod("GET");
            withBody("x");

            filter.doFilter(request, response, downstream(200, "ok", null));

            verify(store, never()).claim(any());
        }

        @Test
        void multipartSkips() throws Exception {
            request.setContentType("multipart/form-data; boundary=xyz");
            withBody("x");

            filter.doFilter(request, response, downstream(200, "ok", null));

            verify(store, never()).claim(any());
        }

        @Test
        void unmatchedPathSkips() throws Exception {
            request.setRequestURI("/v2/nope");
            request.addHeader("Idempotency-Key", "k");

            filter.doFilter(request, response, downstream(200, "ok", null));

            verify(store, never()).claim(any());
        }
    }

    @Nested
    class MissingKey {

        @Test
        void optionalPathPassesThrough() throws Exception {
            request.setRequestURI("/api/v1/users");
            withBody("{}");

            filter.doFilter(request, response, downstream(200, "created", null));

            assertThat(response.getContentAsString()).isEqualTo("created");
            verify(store, never()).claim(any());
        }

        @Test
        void requiredGlobalRejects() throws Exception {
            filter = new IdempotencyFilter(
                    new IdempotencyProperties(
                            true,
                            true,
                            "Idempotency-Key",
                            List.of("/api/v1/**"),
                            List.of(),
                            List.of("POST"),
                            Duration.ofHours(1),
                            65536,
                            true,
                            Duration.ofMinutes(1)),
                    store,
                    (a, b, c, d) -> errors.add(new String[] {c.name(), d}),
                    scopeResolver);
            withBody("{}");

            filter.doFilter(request, response, downstream(200, "ok", null));

            assertThat(errors).hasSize(1);
            assertThat(errors.get(0)[0]).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED.name());
        }

        @Test
        void requiredPathRejects() throws Exception {
            request.setRequestURI("/api/v1/payments");
            withBody("{}");

            filter.doFilter(request, response, downstream(200, "ok", null));

            assertThat(errors.get(0)[0]).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED.name());
            verify(store, never()).claim(any());
        }
    }

    @Nested
    class WithKey {

        @BeforeEach
        void key() {
            request.addHeader("Idempotency-Key", "key-123");
            withBody("{\"amount\":1}");
        }

        @Test
        void tooLongKeyRejected() throws Exception {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 129; i++) {
                sb.append("a");
            }
            request.removeHeader("Idempotency-Key");
            request.addHeader("Idempotency-Key", sb.toString());

            filter.doFilter(request, response, downstream(200, "ok", null));

            assertThat(errors.get(0)[0]).isEqualTo(ErrorCode.BAD_REQUEST.name());
            verify(store, never()).claim(any());
        }

        @Test
        void replayReturnsStoredResponse() throws Exception {
            StoredResponse stored = new StoredResponse(201, "application/json", "{\"id\":9}");
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Replay(stored));

            filter.doFilter(request, response, downstream(200, "should-not-run", null));

            assertThat(response.getStatus()).isEqualTo(201);
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(response.getHeader(IdempotencyFilter.REPLAYED_HEADER)).isEqualTo("true");
            assertThat(response.getContentAsString()).isEqualTo("{\"id\":9}");
        }

        @Test
        void inProgressRejected() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.InProgress());

            filter.doFilter(request, response, downstream(200, "no", null));

            assertThat(errors.get(0)[0]).isEqualTo(ErrorCode.IDEMPOTENCY_IN_PROGRESS.name());
        }

        @Test
        void mismatchRejected() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Mismatch());

            filter.doFilter(request, response, downstream(200, "no", null));

            assertThat(errors.get(0)[0]).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED.name());
        }

        @Test
        void claimedSuccessStoresResult() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(42L));
            List<byte[]> seen = new ArrayList<>();

            filter.doFilter(request, response, downstream(200, "{\"id\":1}", seen));

            assertThat(seen.get(0))
                    .as("downstream reads the fingerprinted body")
                    .isEqualTo("{\"amount\":1}".getBytes(StandardCharsets.UTF_8));
            ArgumentCaptor<StoredResponse> captor = ArgumentCaptor.forClass(StoredResponse.class);
            verify(store).complete(eq(42L), captor.capture());
            assertThat(captor.getValue().status()).isEqualTo(200);
            assertThat(captor.getValue().body()).isEqualTo("{\"id\":1}");
        }

        @Test
        void serverErrorReleasesClaim() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(7L));

            filter.doFilter(request, response, downstream(500, "boom", null));

            verify(store, never()).complete(anyLong(), any());
            verify(store).release(7L);
        }

        @Test
        void requestTimeoutReleasesClaim() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(8L));

            filter.doFilter(request, response, downstream(408, "timeout", null));

            verify(store, never()).complete(anyLong(), any());
            verify(store).release(8L);
        }

        @Test
        void oversizedResponseReleasesClaim() throws Exception {
            buildFilter(16, (a, b, c, d) -> errors.add(new String[] {c.name(), d}));
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(9L));
            String big = "x".repeat(100);

            filter.doFilter(request, response, downstream(200, big, null));

            verify(store, never()).complete(anyLong(), any());
            verify(store).release(9L);
        }

        @Test
        void releaseFailureDoesNotPropagate() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(10L));
            org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                    .when(store)
                    .release(10L);

            filter.doFilter(request, response, downstream(503, "unavailable", null));

            assertThat(response.getStatus()).isEqualTo(503);
            verify(store).release(10L);
        }

        @Test
        void declaredLengthOverCapPassesThrough() throws Exception {
            buildFilter(16, (a, b, c, d) -> errors.add(new String[] {c.name(), d}));
            request.setContent(new byte[100]);

            filter.doFilter(request, response, downstream(200, "ok", null));

            verify(store, never()).claim(any());
            assertThat(response.getContentAsString()).isEqualTo("ok");
        }

        @Test
        void unknownLengthOverCapReturns413() throws Exception {
            buildFilter(16, (a, b, c, d) -> errors.add(new String[] {c.name(), d}));
            MockHttpServletRequest bodyRequest = new MockHttpServletRequest() {
                @Override
                public long getContentLengthLong() {
                    return -1L;
                }
            };
            bodyRequest.setMethod("POST");
            bodyRequest.setRequestURI(PATH);
            bodyRequest.addHeader("Idempotency-Key", "k");
            bodyRequest.setContent(new byte[100]);
            request = bodyRequest;

            filter.doFilter(request, response, downstream(200, "no", null));

            assertThat(errors.get(0)[0]).isEqualTo(ErrorCode.PAYLOAD_TOO_LARGE.name());
            verify(store, never()).claim(any());
        }

        @Test
        void invalidCharsetFallsBackToUtf8() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(11L));
            response.setCharacterEncoding("bogus-charset");

            // Downstream writes no body, so the bogus encoding never reaches a PrintWriter; the filter
            // must still tolerate it when decoding the (empty) captured payload.
            filter.doFilter(request, response, (req, res) -> {
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(200);
            });

            verify(store).complete(eq(11L), any());
        }
    }
}
