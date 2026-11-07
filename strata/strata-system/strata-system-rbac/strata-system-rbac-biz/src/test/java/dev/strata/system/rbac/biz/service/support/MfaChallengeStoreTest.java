// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.strata.common.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

class MfaChallengeStoreTest {

    private final MfaChallengeStore store = new MfaChallengeStore();

    @Test
    void issueThenConsumeReturnsUser() {
        String token = store.issue(42L);
        assertThat(store.consume(token)).isEqualTo(42L);
    }

    @Test
    void challengeIsSingleUse() {
        String token = store.issue(7L);
        store.consume(token);
        assertThatThrownBy(() -> store.consume(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already used");
    }

    @Test
    void unknownTokenRejected() {
        assertThatThrownBy(() -> store.consume("does-not-exist")).isInstanceOf(BusinessException.class);
    }

    @Test
    void blankTokenRejected() {
        assertThatThrownBy(() -> store.consume("  ")).isInstanceOf(BusinessException.class);
    }
}
