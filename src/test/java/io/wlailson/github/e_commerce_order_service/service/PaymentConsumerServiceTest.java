package io.wlailson.github.e_commerce_order_service.service;

import io.wlailson.github.e_commerce_order_service.message.PaymentCreatedMessage;
import io.wlailson.github.e_commerce_order_service.message.PaymentStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentConsumerServiceTest {

    private final OrderService orderService = mock(OrderService.class);
    private final PaymentConsumerService consumer = new PaymentConsumerService(orderService);

    @Nested
    class PaymentCreated {

        @Test
        void forwardsPaymentResultAndPaymentIdToOrderService() {
            PaymentCreatedMessage message = new PaymentCreatedMessage(
                    35L,
                    10L,
                    20L,
                    new BigDecimal("19.99"),
                    PaymentStatus.APPROVED,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            consumer.paymentCreated(message);

            verify(orderService).processPaymentResult(20L, 10L, 35L, PaymentStatus.APPROVED);
        }
    }
}
