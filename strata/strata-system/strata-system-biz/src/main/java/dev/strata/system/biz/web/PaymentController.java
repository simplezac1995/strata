// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.web;

import dev.strata.common.core.web.Result;
import dev.strata.common.security.auth.Permissions;
import dev.strata.system.api.dto.PaymentCreatedDto;
import dev.strata.system.api.dto.PaymentDto;
import dev.strata.system.api.request.CreatePaymentRequest;
import dev.strata.system.biz.demo.PaymentAppService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/demo/payments")
public class PaymentController {

    private final PaymentAppService paymentAppService;

    public PaymentController(PaymentAppService paymentAppService) {
        this.paymentAppService = paymentAppService;
    }

    @GetMapping
    @PreAuthorize(Permissions.DEMO_PAYMENT_LIST)
    public Result<List<PaymentDto>> list() {
        return Result.ok(paymentAppService.list());
    }

    @GetMapping("/{id}")
    @PreAuthorize(Permissions.DEMO_PAYMENT_LIST)
    public Result<PaymentDto> get(@PathVariable Long id) {
        return Result.ok(paymentAppService.get(id));
    }

    @PostMapping
    @PreAuthorize(Permissions.DEMO_PAYMENT_CREATE)
    public Result<PaymentCreatedDto> create(@Valid @RequestBody CreatePaymentRequest request) {
        return Result.ok(paymentAppService.create(request));
    }
}
