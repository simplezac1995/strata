// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.payment.CheckoutSession;
import dev.strata.plugin.api.payment.CreateCheckoutCommand;
import dev.strata.plugin.api.payment.PaymentEvent;
import dev.strata.plugin.api.payment.PaymentGateway;
import java.time.Instant;
import java.util.Optional;

/** Stripe Checkout reference implementation of the PaymentGateway SPI. */
public class StripePaymentGateway implements PaymentGateway {

    private final StripeProperties properties;

    public StripePaymentGateway(StripeProperties properties) {
        this.properties = properties;
        com.stripe.Stripe.apiKey = properties.apiKey();
    }

    @Override
    public CheckoutSession createCheckout(CreateCheckoutCommand command) {
        /* Command values win; plugin-level properties are the fallback. */
        String currency = firstNonBlank(command.currency(), properties.currency());
        String successUrl = firstNonBlank(command.successUrl(), properties.successUrl());
        String cancelUrl = firstNonBlank(command.cancelUrl(), properties.cancelUrl());
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(currency)
                                .setUnitAmount(command.amountCents())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Strata order " + command.idempotencyKey())
                                        .build())
                                .build())
                        .build())
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setClientReferenceId(command.idempotencyKey())
                .setExpiresAt(Instant.now().plusSeconds(1800).getEpochSecond())
                .build();
        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(command.idempotencyKey())
                .build();
        try {
            Session session = Session.create(params, options);
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException ex) {
            /* Never leak provider internals — surface a structured gateway error. */
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Payment gateway unavailable");
        }
    }

    /**
     * Parses and verifies a Stripe webhook. Duplicate deliveries are NOT filtered here —
     * the consuming service must be idempotent (PaymentAppService guards on status), which
     * stays correct across restarts, unlike an in-memory event cache.
     */
    @Override
    public Optional<PaymentEvent> parseWebhook(String payload, String signatureHeader) {
        try {
            Event event = Webhook.constructEvent(payload, signatureHeader, properties.webhookSecret());
            if (!"checkout.session.completed".equals(event.getType())) {
                return Optional.empty();
            }
            EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
            if (deserializer.getObject().isEmpty()) {
                return Optional.empty();
            }
            Session session = (Session) deserializer.getObject().get();
            return Optional.of(new PaymentEvent(event.getId(), session.getId(), "paid"));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid webhook signature");
        }
    }

    private static String firstNonBlank(String primary, String fallback) {
        return primary != null && !primary.isBlank() ? primary : fallback;
    }
}
