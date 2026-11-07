// SPDX-License-Identifier: Apache-2.0
package dev.strata.ai.biz.event;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LinkageTestEventTest {

    @Test
    void record_accessors_work() {
        LinkageTestEvent e = new LinkageTestEvent("user", "login", "detail");
        assertThat(e.object()).isEqualTo("user");
        assertThat(e.event()).isEqualTo("login");
        assertThat(e.detail()).isEqualTo("detail");
        assertThat(e.toString()).contains("LinkageTestEvent");
    }
}
