// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User-Agent 解析测试")
class UserAgentParserTest {

    @Test
    void firefox() {
        assertThat(UserAgentParser.browserOf("Mozilla/5.0 Firefox/128.0")).isEqualTo("Firefox 128.0");
    }

    @Test
    void edge() {
        assertThat(UserAgentParser.browserOf("Mozilla/5.0 Chrome/120.0 Edg/120.0.1"))
                .isEqualTo("Edge 120.0.1");
    }

    @Test
    void chrome() {
        assertThat(UserAgentParser.browserOf("Mozilla/5.0 Chrome/120.0.1 Safari/537.36"))
                .isEqualTo("Chrome 120.0.1");
    }

    @Test
    void safari() {
        assertThat(UserAgentParser.browserOf("Mozilla/5.0 Version/17.2 Safari/605.1.15"))
                .isEqualTo("Safari 17.2");
    }

    @Test
    void blankAndLongFallback() {
        assertThat(UserAgentParser.browserOf("  ")).isNull();
        String longAgent = "x".repeat(200);
        assertThat(UserAgentParser.browserOf(longAgent)).hasSize(128);
    }
}
