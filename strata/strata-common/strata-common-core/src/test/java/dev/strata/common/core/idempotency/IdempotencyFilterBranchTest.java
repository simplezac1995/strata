// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Narrow branch arms of {@link IdempotencyFilter} not in {@link IdempotencyFilterTest}: replay with a
 * blank content type and a null body, response status below 200, and context-path stripping.
 */
class IdempotencyFilterBranchTest {

    @Mock
    private IdempotencyStore store;

    @Mock
    private IdempotencyScopeResolver scopeResolver;

    private AutoCloseable mocks;
    private IdempotencyFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private IdempotencyProperties props() {
        return new IdempotencyProperties(
                true,
                false,
                "Idempotency-Key",
                List.of("/api/v1/**"),
                List.of(),
                List.of("POST"),
                Duration.ofHours(1),
                65536,
                true,
                Duration.ofMinutes(1));
    }

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        when(scopeResolver.resolveScope(any())).thenReturn("user:1");
        request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/api/v1/payments");
        request.addHeader("Idempotency-Key", "k");
        request.setContent("{}".getBytes(StandardCharsets.UTF_8));
        response = new MockHttpServletResponse();
        filter = new IdempotencyFilter(props(), store, (a, b, c, d) -> {}, scopeResolver);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Test
    void replayWithBlankContentTypeAndNullBody() throws Exception {
        when(store.claim(any())).thenReturn(new IdempotencyClaim.Replay(new StoredResponse(204, "  ", null)));
        FilterChain chain = (req, res) -> {
            throw new AssertionError("downstream must not run on replay");
        };

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(response.getContentType()).isNull();
        assertThat(response.getHeader(IdempotencyFilter.REPLAYED_HEADER)).isEqualTo("true");
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void statusBelow200Releases() throws Exception {
        when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(55L));
        FilterChain chain =
                (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(103); // EARLY_HINTS

        filter.doFilter(request, response, chain);

        verify(store).release(55L);
    }

    @Test
    void contextPathStrippedBeforeMatching() throws Exception {
        request.setContextPath("/app");
        request.setRequestURI("/app/api/v1/payments");
        when(store.claim(any())).thenReturn(new IdempotencyClaim.Claimed(77L));
        FilterChain chain = (req, res) -> ((jakarta.servlet.http.HttpServletResponse) res).setStatus(201);

        filter.doFilter(request, response, chain);

        // 剥离后命中 /api/v1/** 规则，正常完成而非跳过
        verify(store).complete(org.mockito.ArgumentMatchers.eq(77L), any());
    }
}
