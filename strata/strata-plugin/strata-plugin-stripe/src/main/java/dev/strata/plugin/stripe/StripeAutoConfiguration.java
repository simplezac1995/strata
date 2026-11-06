// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.stripe;

import dev.strata.plugin.api.payment.PaymentGateway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnProperty(prefix = "strata.plugin.stripe", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(StripeProperties.class)
public class StripeAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "strata.plugin.stripe", name = "api-key", matchIfMissing = false)
    PaymentGateway stripePaymentGateway(StripeProperties properties) {
        return new StripePaymentGateway(properties);
    }
}
