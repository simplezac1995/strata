// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.stripe;

import static org.assertj.core.api.Assertions.assertThat;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.payment.CreateCheckoutCommand;
import org.junit.jupiter.api.Test;

class StripePaymentGatewayTest {

    private StripePaymentGateway gateway() {
        return new StripePaymentGateway(new StripeProperties(
                "sk_test_xxx", "whsec_xxx", "http://localhost:5173/success", "http://localhost:5173/cancel", "usd"));
    }

    @Test
    void parseWebhook_rejects_bad_signature_with_bad_request() {
        BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(
                BusinessException.class, () -> gateway().parseWebhook("{}", "bad-signature"));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void createCheckout_failure_surfaces_gateway_error_without_leaking_provider_details() {
        BusinessException ex = org.junit.jupiter.api.Assertions.assertThrows(BusinessException.class, () -> gateway()
                .createCheckout(new CreateCheckoutCommand("k", 100, "eur", null, null)));

        assertThat(ex.errorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        assertThat(ex.getMessage()).isEqualTo("Payment gateway unavailable");
        assertThat(ex.getMessage()).doesNotContain("Stripe checkout failed");
    }
}
