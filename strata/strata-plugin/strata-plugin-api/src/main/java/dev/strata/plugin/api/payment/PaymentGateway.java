// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.api.payment;

import java.util.Optional;

/**
 * Payment gateway SPI. Stripe starter is the reference implementation.
 */
public interface PaymentGateway {

    CheckoutSession createCheckout(CreateCheckoutCommand command);

    Optional<PaymentEvent> parseWebhook(String payload, String signatureHeader);
}
