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
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("IdempotencyFilter 幂等过滤器")
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
    @DisplayName("shouldNotFilter 旁路条件")
    class Skipping {

        @Test
        @DisplayName("过滤器关闭时直接放行，不接触存储")
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
        @DisplayName("GET 请求不在方法白名单内，直接放行")
        void nonWriteMethodSkips() throws Exception {
            request.setMethod("GET");
            withBody("x");

            filter.doFilter(request, response, downstream(200, "ok", null));

            verify(store, never()).claim(any());
        }

        @Test
        @DisplayName("multipart 上传流式传输，直接放行")
        void multipartSkips() throws Exception {
            request.setContentType("multipart/form-data; boundary=xyz");
            withBody("x");

            filter.doFilter(request, response, downstream(200, "ok", null));

            verify(store, never()).claim(any());
        }

        @Test
        @DisplayName("不匹配任何拦截路径时直接放行")
        void unmatchedPathSkips() throws Exception {
            request.setRequestURI("/v2/nope");
            request.addHeader("Idempotency-Key", "k");

            filter.doFilter(request, response, downstream(200, "ok", null));

            verify(store, never()).claim(any());
        }
    }

    @Nested
    @DisplayName("缺少幂等键")
    class MissingKey {

        @Test
        @DisplayName("非必填路径缺键则放行")
        void optionalPathPassesThrough() throws Exception {
            request.setRequestURI("/api/v1/users");
            withBody("{}");

            filter.doFilter(request, response, downstream(200, "created", null));

            assertThat(response.getContentAsString()).isEqualTo("created");
            verify(store, never()).claim(any());
        }

        @Test
        @DisplayName("required=true 时缺键返回错误")
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
        @DisplayName("requiredPathPatterns 命中时缺键返回错误")
        void requiredPathRejects() throws Exception {
            request.setRequestURI("/api/v1/payments");
            withBody("{}");

            filter.doFilter(request, response, downstream(200, "ok", null));

            assertThat(errors.get(0)[0]).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REQUIRED.name());
            verify(store, never()).claim(any());
        }
    }

    @Nested
    @DisplayName("携带幂等键")
    class WithKey {

        @BeforeEach
        void key() {
            request.addHeader("Idempotency-Key", "key-123");
            withBody("{\"amount\":1}");
        }

        @Test
        @DisplayName("键超长返回 400")
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
        @DisplayName("命中已存响应则原样回放并打 Replayed 头")
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
        @DisplayName("进行中的请求返回 IN_PROGRESS")
        void inProgressRejected() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.InProgress());

            filter.doFilter(request, response, downstream(200, "no", null));

            assertThat(errors.get(0)[0]).isEqualTo(ErrorCode.IDEMPOTENCY_IN_PROGRESS.name());
        }

        @Test
        @DisplayName("键被不同请求复用返回 KEY_REUSED")
        void mismatchRejected() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Mismatch());

            filter.doFilter(request, response, downstream(200, "no", null));

            assertThat(errors.get(0)[0]).isEqualTo(ErrorCode.IDEMPOTENCY_KEY_REUSED.name());
        }

        @Test
        @DisplayName("首次成功：下游收到缓存体，2xx 结果被存储")
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
        @DisplayName("5xx 结果不回放，释放键以便重试")
        void serverErrorReleasesClaim() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(7L));

            filter.doFilter(request, response, downstream(500, "boom", null));

            verify(store, never()).complete(anyLong(), any());
            verify(store).release(7L);
        }

        @Test
        @DisplayName("408 请求超时不回放")
        void requestTimeoutReleasesClaim() throws Exception {
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(8L));

            filter.doFilter(request, response, downstream(408, "timeout", null));

            verify(store, never()).complete(anyLong(), any());
            verify(store).release(8L);
        }

        @Test
        @DisplayName("响应体超过缓存上限不回放")
        void oversizedResponseReleasesClaim() throws Exception {
            buildFilter(16, (a, b, c, d) -> errors.add(new String[] {c.name(), d}));
            when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(9L));
            String big = "x".repeat(100);

            filter.doFilter(request, response, downstream(200, big, null));

            verify(store, never()).complete(anyLong(), any());
            verify(store).release(9L);
        }

        @Test
        @DisplayName("释放失败不影响已渲染的响应（降级路径）")
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
        @DisplayName("声明长度已超限则原样放行、不缓存 body")
        void declaredLengthOverCapPassesThrough() throws Exception {
            buildFilter(16, (a, b, c, d) -> errors.add(new String[] {c.name(), d}));
            request.setContent(new byte[100]);

            filter.doFilter(request, response, downstream(200, "ok", null));

            verify(store, never()).claim(any());
            assertThat(response.getContentAsString()).isEqualTo("ok");
        }

        @Test
        @DisplayName("未知长度 body 读取超限返回 413")
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
        @DisplayName("非法响应字符集回退 UTF-8 后仍存储")
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
