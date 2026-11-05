// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("IdempotencyScopeResolver 幂等键作用域解析")
class IdempotencyScopeResolverTest {

    private final IdempotencyScopeResolver resolver = IdempotencyScopeResolver.principalOrRemoteAddress();

    @Test
    @DisplayName("已认证主体时按用户名划分作用域")
    void usesPrincipalName() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("alice");
        when(request.getUserPrincipal()).thenReturn(principal);

        assertThat(resolver.resolveScope(request)).isEqualTo("user:alice");
    }

    @Test
    @DisplayName("主体存在但名为空时回退到客户端地址")
    void blankPrincipalNameFallsBackToAddress() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(null);
        when(request.getUserPrincipal()).thenReturn(principal);
        when(request.getRemoteAddr()).thenReturn("10.0.0.9");

        assertThat(resolver.resolveScope(request)).isEqualTo("addr:10.0.0.9");
    }

    @Test
    @DisplayName("无主体时按客户端地址划分作用域")
    void usesRemoteAddressWhenAnonymous() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("203.0.113.7");

        assertThat(resolver.resolveScope(request)).isEqualTo("addr:203.0.113.7");
    }

    @Test
    @DisplayName("主体与地址都缺失时记为 unknown，不返回 null")
    void fallsBackToUnknown() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        assertThat(resolver.resolveScope(request)).isEqualTo("addr:unknown");
    }

    @Test
    @DisplayName("超长主体名截断到 64 字符以匹配列宽")
    void truncatesOversizedScope() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("a".repeat(100));
        when(request.getUserPrincipal()).thenReturn(principal);

        String scope = resolver.resolveScope(request);
        assertThat(scope).hasSize(64).startsWith("user:");
    }
}
