// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore.Entry;
import dev.strata.system.rbac.biz.service.support.SocialLoginStateStore.Session;
import org.junit.jupiter.api.Test;

/**
 * P2-9 social-login state/PKCE store: one-time nonce, server-side verifier, replay protection.
 */
class SocialLoginStateStoreTest {

    private final SocialLoginStateStore store = new SocialLoginStateStore();

    @Test
    void issueMintsMaterial() {
        Session s = store.issue("google", "https://app.example.com/callback");
        assertThat(s.state()).isNotBlank();
        assertThat(s.codeVerifier()).isNotBlank();
        assertThat(s.codeChallenge()).isNotBlank();
        assertThat(s.state()).isNotEqualTo(s.codeVerifier());
    }

    @Test
    void consumeReturnsAndRemoves() {
        Session s = store.issue("google", "https://app.example.com/callback");
        Entry e = store.consume(s.state());
        assertThat(e.codeVerifier()).isEqualTo(s.codeVerifier());
        assertThat(e.redirectUri()).isEqualTo("https://app.example.com/callback");
        assertThat(e.provider()).isEqualTo("google");
        // second consume must fail: state is single-use
        assertThatThrownBy(() -> store.consume(s.state())).isInstanceOf(BusinessException.class);
    }

    @Test
    void unknownStateRejected() {
        assertThatThrownBy(() -> store.consume("nope")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> store.consume("")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> store.consume(null)).isInstanceOf(BusinessException.class);
    }
}
