// SPDX-License-Identifier: Apache-2.0
package dev.strata.system.biz.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.strata.common.core.exception.BusinessException;
import dev.strata.common.core.exception.ErrorCode;
import dev.strata.plugin.api.payment.CheckoutSession;
import dev.strata.plugin.api.payment.CreateCheckoutCommand;
import dev.strata.plugin.api.payment.PaymentGateway;
import dev.strata.system.api.dto.PaymentCreatedDto;
import dev.strata.system.api.dto.PaymentDto;
import dev.strata.system.api.entity.DemoPayment;
import dev.strata.system.api.request.CreatePaymentRequest;
import dev.strata.system.biz.repository.DemoPaymentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentAppService 单元测试")
class PaymentAppServiceTest {

    @Mock
    private DemoPaymentRepository demoPaymentRepository;

    @Mock
    private ObjectProvider<PaymentGateway> paymentGatewayProvider;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentMapper paymentMapper;

    private PaymentAppService service;

    @BeforeEach
    void setUp() {
        service = new PaymentAppService(demoPaymentRepository, paymentGatewayProvider, paymentMapper);
    }

    @Nested
    @DisplayName("create - 创建支付")
    class CreateTests {

        @Test
        @DisplayName("未配置网关时抛 CONFLICT")
        void create_NoGateway_ThrowsConflict() {
            when(paymentGatewayProvider.getIfAvailable()).thenReturn(null);
            CreatePaymentRequest request = new CreatePaymentRequest(500L, "usd");

            assertThatThrownBy(() -> service.create(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.CONFLICT));

            verify(demoPaymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("货币为空时默认使用 usd")
        void create_BlankCurrency_DefaultsToUsd() {
            stubGateway();
            when(demoPaymentRepository.save(any(DemoPayment.class))).thenAnswer(inv -> {
                DemoPayment p = inv.getArgument(0);
                p.setId(11L);
                return p;
            });
            CreatePaymentRequest request = new CreatePaymentRequest(500L, "  ");

            PaymentCreatedDto result = service.create(request);

            assertThat(result).isNotNull();
            ArgumentCaptor<CreateCheckoutCommand> cmdCaptor = ArgumentCaptor.forClass(CreateCheckoutCommand.class);
            verify(paymentGateway).createCheckout(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().currency()).isEqualTo("usd");
        }

        @Test
        @DisplayName("货币非空时转小写后传给网关")
        void create_WithCurrency_LowercasesCurrency() {
            stubGateway();
            when(demoPaymentRepository.save(any(DemoPayment.class))).thenAnswer(inv -> {
                DemoPayment p = inv.getArgument(0);
                p.setId(12L);
                return p;
            });
            CreatePaymentRequest request = new CreatePaymentRequest(1000L, "EUR");

            service.create(request);

            ArgumentCaptor<CreateCheckoutCommand> cmdCaptor = ArgumentCaptor.forClass(CreateCheckoutCommand.class);
            verify(paymentGateway).createCheckout(cmdCaptor.capture());
            assertThat(cmdCaptor.getValue().currency()).isEqualTo("eur");
            assertThat(cmdCaptor.getValue().amountCents()).isEqualTo(1000L);
        }

        private void stubGateway() {
            when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
            when(paymentGateway.createCheckout(any(CreateCheckoutCommand.class)))
                    .thenReturn(new CheckoutSession("cs_1", "https://checkout.example/cs_1"));
        }
    }

    @Nested
    @DisplayName("list - 支付列表")
    class ListTests {

        @Test
        @DisplayName("多条记录映射为 DTO 列表")
        void list_ReturnsMappedList() {
            DemoPayment p = new DemoPayment();
            p.setId(1L);
            when(demoPaymentRepository.findAllByOrderByIdDesc()).thenReturn(List.of(p));
            when(paymentMapper.toDto(p)).thenReturn(new PaymentDto("1", "ORD-1", 100L, "usd", "PENDING"));

            List<PaymentDto> result = service.list();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).orderNo()).isEqualTo("ORD-1");
        }

        @Test
        @DisplayName("空库返回空列表")
        void list_Empty_ReturnsEmptyList() {
            when(demoPaymentRepository.findAllByOrderByIdDesc()).thenReturn(List.of());

            List<PaymentDto> result = service.list();

            assertThat(result).isNotNull().isEmpty();
        }
    }

    @Nested
    @DisplayName("get - 支付详情")
    class GetTests {

        @Test
        @DisplayName("存在时返回 DTO")
        void get_Exists_ReturnsDto() {
            DemoPayment p = new DemoPayment();
            p.setId(3L);
            when(demoPaymentRepository.findById(3L)).thenReturn(Optional.of(p));
            when(paymentMapper.toDto(p)).thenReturn(new PaymentDto("3", "ORD-3", 200L, "usd", "PAID"));

            PaymentDto result = service.get(3L);

            assertThat(result.orderNo()).isEqualTo("ORD-3");
        }

        @Test
        @DisplayName("不存在时抛 NOT_FOUND")
        void get_NotExists_ThrowsNotFound() {
            when(demoPaymentRepository.findById(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.get(404L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex ->
                            assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("onPaymentCompleted - 支付结果回调")
    class OnCompletedTests {

        @Test
        @DisplayName("PAID 状态推进为 PAID 并落库")
        void on_Paid_SetsPaid() {
            DemoPayment p = pendingPayment();
            when(demoPaymentRepository.findBySessionId("cs_1")).thenReturn(Optional.of(p));

            service.onPaymentCompleted("cs_1", "PAID");

            assertThat(p.getStatus()).isEqualTo(PaymentAppService.STATUS_PAID);
            verify(demoPaymentRepository).save(p);
        }

        @Test
        @DisplayName("非 PAID 状态推进为 FAILED 并落库")
        void on_Failed_SetsFailed() {
            DemoPayment p = pendingPayment();
            when(demoPaymentRepository.findBySessionId("cs_2")).thenReturn(Optional.of(p));

            service.onPaymentCompleted("cs_2", "EXPIRED");

            assertThat(p.getStatus()).isEqualTo(PaymentAppService.STATUS_FAILED);
            verify(demoPaymentRepository).save(p);
        }

        @Test
        @DisplayName("当前状态已是目标状态时跳过落库（幂等）")
        void on_AlreadyTarget_NoSave() {
            DemoPayment p = pendingPayment();
            p.setStatus(PaymentAppService.STATUS_PAID);
            when(demoPaymentRepository.findBySessionId("cs_3")).thenReturn(Optional.of(p));

            service.onPaymentCompleted("cs_3", "PAID");

            verify(demoPaymentRepository, never()).save(any());
        }

        @Test
        @DisplayName("会话不存在时无副作用")
        void on_UnknownSession_Noop() {
            when(demoPaymentRepository.findBySessionId("cs_ghost")).thenReturn(Optional.empty());

            service.onPaymentCompleted("cs_ghost", "PAID");

            verify(demoPaymentRepository, never()).save(any());
        }

        private DemoPayment pendingPayment() {
            DemoPayment p = new DemoPayment();
            p.setId(9L);
            p.setStatus(PaymentAppService.STATUS_PENDING);
            return p;
        }
    }

    @Nested
    @DisplayName("create - 成功路径（覆盖 checkoutUrl 返回）")
    class CreateSuccessTests {

        @Test
        @DisplayName("创建成功返回订单号与结账链接")
        void create_Success_ReturnsOrderAndUrl() {
            when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
            when(paymentGateway.createCheckout(any(CreateCheckoutCommand.class)))
                    .thenReturn(new CheckoutSession("cs_ok", "https://checkout.example/cs_ok"));
            when(demoPaymentRepository.save(any(DemoPayment.class))).thenAnswer(inv -> {
                DemoPayment p = inv.getArgument(0);
                p.setId(42L);
                return p;
            });

            PaymentCreatedDto result = service.create(new CreatePaymentRequest(999L, "JPY"));

            assertThat(result.checkoutUrl()).isEqualTo("https://checkout.example/cs_ok");
            assertThat(result.orderNo()).startsWith("ORD-");
            assertThat(result.id()).isEqualTo("42");
            verify(demoPaymentRepository, times(2)).save(any(DemoPayment.class));
        }
    }

    @Nested
    @DisplayName("create - 网关抛错时不吞掉")
    class CreateErrorTests {

        @Test
        @DisplayName("网关抛异常时向上传播")
        void create_GatewayThrows_Propagates() {
            when(paymentGatewayProvider.getIfAvailable()).thenReturn(paymentGateway);
            when(demoPaymentRepository.save(any(DemoPayment.class))).thenAnswer(inv -> {
                DemoPayment p = inv.getArgument(0);
                p.setId(1L);
                return p;
            });
            when(paymentGateway.createCheckout(any(CreateCheckoutCommand.class)))
                    .thenThrow(new RuntimeException("stripe down"));

            org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class, () -> service.create(new CreatePaymentRequest(100L, null)));
        }
    }
}
