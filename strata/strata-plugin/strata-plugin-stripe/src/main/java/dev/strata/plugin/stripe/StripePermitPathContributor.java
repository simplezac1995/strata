// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.stripe;

import dev.strata.common.core.security.PermitPathContributor;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Declares the Stripe webhook as unauthenticated (ADR-0021).
 *
 * <p>Registered only while the Stripe module is enabled — mirroring {@link StripeAutoConfiguration} —
 * so disabling the module removes the path from the security surface. The webhook endpoint itself
 * exists only when an API key and webhook secret are configured, so an enabled-but-unconfigured
 * module yields a permit path with no mapping (404), which is safe.
 */
@Component
@ConditionalOnProperty(prefix = "strata.plugin.stripe", name = "enabled", havingValue = "true")
public class StripePermitPathContributor implements PermitPathContributor {

    @Override
    public List<String> permitPaths() {
        return List.of("/api/v1/plugins/stripe/webhook");
    }
}
