// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.stripe;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "strata.plugin.stripe")
public record StripeProperties(
        String apiKey, String webhookSecret, String successUrl, String cancelUrl, String currency) {

    public StripeProperties {
        if (currency == null || currency.isBlank()) {
            currency = "usd";
        }
        if (successUrl == null || successUrl.isBlank()) {
            successUrl = "http://localhost:5173/demo/payments?success=1";
        }
        if (cancelUrl == null || cancelUrl.isBlank()) {
            cancelUrl = "http://localhost:5173/demo/payments?canceled=1";
        }
    }
}
