// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.strata.common.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MfaChallengeStore 单次挑战令牌测试")
class MfaChallengeStoreTest {

    private final MfaChallengeStore store = new MfaChallengeStore();

    @Test
    @DisplayName("签发后可消费一次并返回绑定用户")
    void issueThenConsumeReturnsUser() {
        String token = store.issue(42L);
        assertThat(store.consume(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("同一挑战令牌不能二次消费(防重放)")
    void challengeIsSingleUse() {
        String token = store.issue(7L);
        store.consume(token);
        assertThatThrownBy(() -> store.consume(token))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already used");
    }

    @Test
    @DisplayName("未知令牌被拒绝")
    void unknownTokenRejected() {
        assertThatThrownBy(() -> store.consume("does-not-exist")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("空令牌被拒绝")
    void blankTokenRejected() {
        assertThatThrownBy(() -> store.consume("  ")).isInstanceOf(BusinessException.class);
    }
}
