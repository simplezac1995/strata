// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.rbac.biz.saml;

import dev.strata.common.core.security.PermitPathContributor;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** SAML endpoints must be reachable before a JWT exists (config, SP redirect, ACS, one-time exchange). */
@Component
@ConditionalOnProperty(prefix = "strata.saml", name = "enabled", havingValue = "true")
public class SamlPermitPathContributor implements PermitPathContributor {

    @Override
    public List<String> permitPaths() {
        return List.of(
                "/api/v1/system/auth/saml/config",
                "/api/v1/system/auth/saml/exchange",
                "/saml2/authenticate",
                "/saml2/acs");
    }
}
