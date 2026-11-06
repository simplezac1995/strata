// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.strata.common.core.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SAML one-time token store 测试")
class OneTimeTokenStoreTest {

    @Test
    @DisplayName("memory store 单次消费且重复使用被拒绝")
    void memoryStoreIsSingleUse() {
        OneTimeTokenStore store = new OneTimeTokenStore("test relay state", "memory", null, "test:", 60);
        String key = store.newKey();
        store.put(key, "request-id");

        assertThat(store.take(key)).isEqualTo("request-id");
        assertThatThrownBy(() -> store.take(key)).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("空白 key 直接拒绝")
    void blankKeyRejected() {
        OneTimeTokenStore store = new OneTimeTokenStore("test relay state", "memory", null, "test:", 60);
        assertThatThrownBy(() -> store.take(" ")).isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("redis 模式缺少模板时启动失败")
    void redisRequiresTemplate() {
        assertThatThrownBy(() -> new OneTimeTokenStore("test", "redis", null, "test:", 60))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("StringRedisTemplate");
    }
}
