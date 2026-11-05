// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.demo;

import dev.strata.plugin.api.payment.PaymentEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {

    private final PaymentAppService paymentAppService;

    public PaymentEventListener(PaymentAppService paymentAppService) {
        this.paymentAppService = paymentAppService;
    }

    @EventListener
    public void onPaymentEvent(PaymentEvent event) {
        paymentAppService.onPaymentCompleted(event.sessionId(), event.status());
    }
}
