// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.stripe.web;

import dev.strata.common.core.web.Result;
import dev.strata.plugin.api.payment.PaymentGateway;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stripe webhook endpoint; signature-verified, publishes {@code PaymentEvent} for business listeners.
 *
 * <p>Registered by component scan while a webhook secret is configured ({@code @ConditionalOnProperty}).
 * The {@link PaymentGateway} is resolved lazily via {@link ObjectProvider} — the gateway bean itself is
 * created by {@link StripeAutoConfiguration} only when an API key exists, so controller registration must
 * not depend on bean-registration order (a scanned {@code @ConditionalOnBean} would evaluate too early).
 * Without a gateway the endpoint answers 500 (misconfiguration: secret without api-key).
 */
@RestController
@ConditionalOnProperty(prefix = "strata.plugin.stripe", name = "webhook-secret", matchIfMissing = false)
public class StripeWebhookController {

    public static final String SIGNATURE_HEADER = "Stripe-Signature";

    private final ObjectProvider<PaymentGateway> gatewayProvider;
    private final ApplicationEventPublisher eventPublisher;

    public StripeWebhookController(
            ObjectProvider<PaymentGateway> gatewayProvider, ApplicationEventPublisher eventPublisher) {
        this.gatewayProvider = gatewayProvider;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/api/v1/plugins/stripe/webhook")
    public Result<Void> webhook(@RequestBody String payload, @RequestHeader(SIGNATURE_HEADER) String signature) {
        PaymentGateway gateway = gatewayProvider.getIfAvailable();
        if (gateway == null) {
            throw new IllegalStateException(
                    "Stripe gateway not configured; set strata.plugin.stripe.api-key to enable webhook processing");
        }
        gateway.parseWebhook(payload, signature).ifPresent(eventPublisher::publishEvent);
        return Result.ok();
    }
}
