// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.security.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

@DisplayName("TenantContextFilter 租户上下文过滤器")
class TenantContextFilterTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    private FilterChain chainCapturing(AtomicReference<Long> seen) throws Exception {
        FilterChain chain = mock(FilterChain.class);
        doAnswer(invocation -> {
                    seen.set(TenantContext.getTenantId());
                    return null;
                })
                .when(chain)
                .doFilter(any(), any());
        return chain;
    }

    @Test
    @DisplayName("TenantProperties 默认关闭行级租户")
    void propertiesDisabledByDefault() {
        assertThat(new TenantProperties().isRowLevelEnabled()).isFalse();
    }

    @Test
    @DisplayName("关闭时直接放行且不读取租户头")
    void disabledPassesThroughWithoutReadingHeader() throws Exception {
        TenantProperties properties = new TenantProperties();
        properties.setRowLevelEnabled(false);
        TenantContextFilter filter = new TenantContextFilter(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        AtomicReference<Long> seen = new AtomicReference<>(-1L);
        FilterChain chain = chainCapturing(seen);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(request, never()).getHeader(any());
        assertThat(seen.get()).isNull();
    }

    @Test
    @DisplayName("开启但无租户头时不设置租户，请求后上下文清空")
    void enabledWithoutHeaderLeavesContextEmpty() throws Exception {
        TenantProperties properties = new TenantProperties();
        properties.setRowLevelEnabled(true);
        TenantContextFilter filter = new TenantContextFilter(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(TenantContextFilter.HEADER_TENANT_ID)).thenReturn(null);
        AtomicReference<Long> seen = new AtomicReference<>(-1L);
        FilterChain chain = chainCapturing(seen);

        filter.doFilter(request, mock(HttpServletResponse.class), chain);

        assertThat(seen.get()).isNull();
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("开启且带租户头时在请求内绑定租户，请求后清理")
    void bindsTenantForRequestThenClears() throws Exception {
        TenantProperties properties = new TenantProperties();
        properties.setRowLevelEnabled(true);
        TenantContextFilter filter = new TenantContextFilter(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(TenantContextFilter.HEADER_TENANT_ID)).thenReturn(" 42 ");
        AtomicReference<Long> seen = new AtomicReference<>();
        FilterChain chain = chainCapturing(seen);

        filter.doFilter(request, mock(HttpServletResponse.class), chain);

        assertThat(seen.get()).isEqualTo(42L);
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    @DisplayName("租户头为空白时不绑定租户")
    void blankHeaderDoesNotBindTenant() throws Exception {
        TenantProperties properties = new TenantProperties();
        properties.setRowLevelEnabled(true);
        TenantContextFilter filter = new TenantContextFilter(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(TenantContextFilter.HEADER_TENANT_ID)).thenReturn("   ");
        AtomicReference<Long> seen = new AtomicReference<>(-1L);
        FilterChain chain = chainCapturing(seen);

        filter.doFilter(request, mock(HttpServletResponse.class), chain);

        assertThat(seen.get()).isNull();
    }

    @Test
    @DisplayName("租户头非法时抛出 NumberFormatException 且仍清理上下文、不放行")
    void invalidHeaderThrowsAndClearsContext() throws Exception {
        TenantProperties properties = new TenantProperties();
        properties.setRowLevelEnabled(true);
        TenantContextFilter filter = new TenantContextFilter(properties);

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(TenantContextFilter.HEADER_TENANT_ID)).thenReturn("abc");
        FilterChain chain = mock(FilterChain.class);

        assertThatThrownBy(() -> filter.doFilter(request, mock(HttpServletResponse.class), chain))
                .isInstanceOf(NumberFormatException.class);

        assertThat(TenantContext.getTenantId()).isNull();
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("自动装配以高优先级注册租户过滤器")
    void autoConfigurationRegistersFilterWithHighPrecedence() {
        TenantProperties properties = new TenantProperties();
        FilterRegistrationBean<TenantContextFilter> registration =
                new TenantAutoConfiguration().tenantContextFilter(properties);

        assertThat(registration.getFilter()).isInstanceOf(TenantContextFilter.class);
        assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 20);
    }
}
