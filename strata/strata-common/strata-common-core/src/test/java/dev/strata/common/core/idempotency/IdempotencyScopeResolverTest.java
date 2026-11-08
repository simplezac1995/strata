// SPDX-License-Identifier: Apache-2.0
package dev.strata.common.core.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.junit.jupiter.api.Test;

class IdempotencyScopeResolverTest {

    private final IdempotencyScopeResolver resolver = IdempotencyScopeResolver.principalOrRemoteAddress();

    @Test
    void usesPrincipalName() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("alice");
        when(request.getUserPrincipal()).thenReturn(principal);

        assertThat(resolver.resolveScope(request)).isEqualTo("user:alice");
    }

    @Test
    void blankPrincipalNameFallsBackToAddress() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn(null);
        when(request.getUserPrincipal()).thenReturn(principal);
        when(request.getRemoteAddr()).thenReturn("10.0.0.9");

        assertThat(resolver.resolveScope(request)).isEqualTo("addr:10.0.0.9");
    }

    @Test
    void usesRemoteAddressWhenAnonymous() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("203.0.113.7");

        assertThat(resolver.resolveScope(request)).isEqualTo("addr:203.0.113.7");
    }

    @Test
    void fallsBackToUnknown() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        assertThat(resolver.resolveScope(request)).isEqualTo("addr:unknown");
    }

    @Test
    void truncatesOversizedScope() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("a".repeat(100));
        when(request.getUserPrincipal()).thenReturn(principal);

        String scope = resolver.resolveScope(request);
        assertThat(scope).hasSize(64).startsWith("user:");
    }
}
