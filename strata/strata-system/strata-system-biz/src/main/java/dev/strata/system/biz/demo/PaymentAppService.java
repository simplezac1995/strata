// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.demo;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.common.core.exception.NotFoundException;
import dev.strata.plugin.api.payment.CheckoutSession;
import dev.strata.plugin.api.payment.CreateCheckoutCommand;
import dev.strata.plugin.api.payment.PaymentGateway;
import dev.strata.system.api.dto.PaymentCreatedDto;
import dev.strata.system.api.dto.PaymentDto;
import dev.strata.system.api.entity.DemoPayment;
import dev.strata.system.api.request.CreatePaymentRequest;
import dev.strata.system.biz.repository.DemoPaymentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** W5 demo: Stripe Checkout via {@link PaymentGateway} SPI ({@code demo:payment:*} resources). */
@Service
public class PaymentAppService {

    private static final Logger log = LoggerFactory.getLogger(PaymentAppService.class);

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_FAILED = "FAILED";

    private final DemoPaymentRepository demoPaymentRepository;
    private final ObjectProvider<PaymentGateway> paymentGatewayProvider;
    private final PaymentMapper paymentMapper;

    public PaymentAppService(
            DemoPaymentRepository demoPaymentRepository,
            ObjectProvider<PaymentGateway> paymentGatewayProvider,
            PaymentMapper paymentMapper) {
        this.demoPaymentRepository = demoPaymentRepository;
        this.paymentGatewayProvider = paymentGatewayProvider;
        this.paymentMapper = paymentMapper;
    }

    @Transactional
    public PaymentCreatedDto create(CreatePaymentRequest request) {
        PaymentGateway gateway = paymentGatewayProvider.getIfAvailable();
        if (gateway == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "No payment gateway configured");
        }
        String orderNo = "ORD-" + Instant.now().toEpochMilli() + "-" + (int) (Math.random() * 9000 + 1000);
        String currency = (request.currency() == null || request.currency().isBlank())
                ? "usd"
                : request.currency().toLowerCase(Locale.ROOT);
        DemoPayment payment = new DemoPayment();
        payment.setOrderNo(orderNo);
        payment.setAmountCents(request.amountCents());
        payment.setCurrency(currency);
        payment.setStatus(STATUS_PENDING);
        demoPaymentRepository.save(payment);

        CheckoutSession session =
                gateway.createCheckout(new CreateCheckoutCommand(orderNo, request.amountCents(), currency, null, null));
        payment.setSessionId(session.sessionId());
        demoPaymentRepository.save(payment);
        return new PaymentCreatedDto(String.valueOf(payment.getId()), orderNo, session.checkoutUrl());
    }

    @Transactional(readOnly = true)
    public List<PaymentDto> list() {
        return demoPaymentRepository.findAllByOrderByIdDesc().stream()
                .map(paymentMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentDto get(Long id) {
        return paymentMapper.toDto(require(id));
    }

    @Transactional
    public void onPaymentCompleted(String sessionId, String status) {
        demoPaymentRepository.findBySessionId(sessionId).ifPresent(payment -> {
            String target = STATUS_PAID.equals(status) ? STATUS_PAID : STATUS_FAILED;
            if (target.equals(payment.getStatus())) {
                return;
            }
            payment.setStatus(target);
            demoPaymentRepository.save(payment);
        });
    }

    private DemoPayment require(Long id) {
        return demoPaymentRepository.findById(id).orElseThrow(() -> new NotFoundException("Payment not found"));
    }
}
