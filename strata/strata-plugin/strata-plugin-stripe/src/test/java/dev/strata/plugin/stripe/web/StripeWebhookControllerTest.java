// SPDX-License-Identifier: Apache-2.0
package dev.strata.plugin.stripe.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.GlobalExceptionHandler;
import dev.strata.plugin.api.payment.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class StripeWebhookControllerTest {

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectProvider<PaymentGateway> gatewayProvider = new ObjectProvider<PaymentGateway>() {
            @Override
            public PaymentGateway getObject() {
                return paymentGateway;
            }

            @Override
            public PaymentGateway getIfAvailable() {
                return paymentGateway;
            }

            @Override
            public PaymentGateway getIfUnique() {
                return paymentGateway;
            }
        };
        mockMvc = MockMvcBuilders.standaloneSetup(new StripeWebhookController(gatewayProvider, eventPublisher))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void webhook_bad_signature_returns_400() throws Exception {
        when(paymentGateway.parseWebhook(anyString(), eq("bad-signature")))
                .thenThrow(new BusinessException(ErrorCode.BAD_REQUEST, "Invalid Stripe signature"));

        mockMvc.perform(post("/api/v1/plugins/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(StripeWebhookController.SIGNATURE_HEADER, "bad-signature")
                        .content("{\"id\":\"evt_test\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }
}
