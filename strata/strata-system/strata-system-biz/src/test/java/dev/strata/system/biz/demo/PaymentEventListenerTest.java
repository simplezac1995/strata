// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.demo;

import static org.mockito.Mockito.verify;

import dev.strata.plugin.api.payment.PaymentEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentEventListenerTest {

    @Mock
    private PaymentAppService paymentAppService;

    @Test
    void onPaymentEvent_DelegatesToAppService() {
        PaymentEventListener listener = new PaymentEventListener(paymentAppService);
        PaymentEvent event = new PaymentEvent("evt-1", "cs-9", "PAID");

        listener.onPaymentEvent(event);

        verify(paymentAppService).onPaymentCompleted("cs-9", "PAID");
    }
}
