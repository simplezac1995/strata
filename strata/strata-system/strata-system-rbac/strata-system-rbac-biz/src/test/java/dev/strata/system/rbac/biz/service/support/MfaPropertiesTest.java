// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.service.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MfaPropertiesTest {

    @Test
    void defaultsToTheStrataIssuer() {
        assertThat(new MfaProperties().getIssuer()).isEqualTo("Strata Admin");
    }

    @Test
    void storesTheConfiguredIssuer() {
        MfaProperties properties = new MfaProperties();
        properties.setIssuer("Acme Admin");
        assertThat(properties.getIssuer()).isEqualTo("Acme Admin");
    }
}
