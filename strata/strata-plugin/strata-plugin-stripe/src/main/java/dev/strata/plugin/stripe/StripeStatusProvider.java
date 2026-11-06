// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.stripe;

import dev.strata.plugin.api.payment.PaymentGateway;
import dev.strata.plugin.api.status.PluginStatusProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Self-declares the {@code paymentStripe} status key (ADR-0020).
 *
 * <p>Registered only while the Stripe module is enabled — mirroring {@link StripeAutoConfiguration}
 * — so the key is omitted from {@code /plugins/status} when the switch is off. {@link
 * StripePaymentGateway} is only created when an API key is configured, so bean presence already
 * expresses "enabled and API key present".
 */
@Component
@Order(20)
@ConditionalOnProperty(prefix = "strata.plugin.stripe", name = "enabled", havingValue = "true")
public class StripeStatusProvider implements PluginStatusProvider {

    private final ObjectProvider<PaymentGateway> paymentGateway;

    public StripeStatusProvider(ObjectProvider<PaymentGateway> paymentGateway) {
        this.paymentGateway = paymentGateway;
    }

    @Override
    public String key() {
        return "paymentStripe";
    }

    @Override
    public String label() {
        return "Stripe payments";
    }

    @Override
    public boolean available() {
        return paymentGateway.getIfAvailable() != null;
    }

    @Override
    public String detail() {
        return "Stripe Checkout gateway";
    }
}
