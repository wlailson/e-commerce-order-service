package io.wlailson.github.e_commerce_order_service.service;

import io.wlailson.github.e_commerce_order_service.domain.Order;
import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;
import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.message.KafkaProducerTopics;
import io.wlailson.github.e_commerce_order_service.message.PaymentStatus;
import io.wlailson.github.e_commerce_order_service.repository.OrderRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServicePaymentResultTest {

    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderStateService stateService = mock(OrderStateService.class);
    private final KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);
    private final KafkaProducerTopics kafkaProducerTopics = mock(KafkaProducerTopics.class);
    private final OrderService service = new OrderService(
            repository, stateService, kafkaProducerService, kafkaProducerTopics);

    @Nested
    class ProcessPaymentResult {

        @Test
        void approvedPaymentAssociatesPaymentIdAndMovesOrderToPaid() {
            Order order = order(OrderStatus.CREATED, null);
            when(repository.findByClientIdAndId(20L, 10L)).thenReturn(Optional.of(order));
            when(stateService.processEvent(OrderStatus.CREATED, OrderEvent.PAY)).thenReturn(OrderStatus.PAID);

            service.processPaymentResult(20L, 10L, 35L, PaymentStatus.APPROVED);

            assertEquals(35L, order.getPaymentId());
            assertEquals(OrderStatus.PAID, order.getStatus());
            verify(repository).save(order);
            verify(stateService).processEvent(OrderStatus.CREATED, OrderEvent.PAY);
        }

        @Test
        void refusedPaymentAssociatesPaymentIdAndCancelsOrder() {
            Order order = order(OrderStatus.CREATED, null);
            when(repository.findByClientIdAndId(20L, 10L)).thenReturn(Optional.of(order));
            when(stateService.processEvent(OrderStatus.CREATED, OrderEvent.CANCEL)).thenReturn(OrderStatus.CANCELLED);

            service.processPaymentResult(20L, 10L, 35L, PaymentStatus.REFUSED);

            assertEquals(35L, order.getPaymentId());
            assertEquals(OrderStatus.CANCELLED, order.getStatus());
            verify(repository).save(order);
            verify(stateService).processEvent(OrderStatus.CREATED, OrderEvent.CANCEL);
        }

        @Test
        void duplicateApprovedEventDoesNotRepeatStateTransition() {
            Order order = order(OrderStatus.PAID, 35L);
            when(repository.findByClientIdAndId(20L, 10L)).thenReturn(Optional.of(order));

            service.processPaymentResult(20L, 10L, 35L, PaymentStatus.APPROVED);

            assertEquals(OrderStatus.PAID, order.getStatus());
            verify(stateService, never()).processEvent(OrderStatus.PAID, OrderEvent.PAY);
            verify(repository).save(order);
        }

        @Test
        void nonFinalPaymentStatusAssociatesPaymentWithoutChangingOrderState() {
            Order order = order(OrderStatus.CREATED, null);
            when(repository.findByClientIdAndId(20L, 10L)).thenReturn(Optional.of(order));

            service.processPaymentResult(20L, 10L, 35L, PaymentStatus.PENDING);

            assertEquals(35L, order.getPaymentId());
            assertEquals(OrderStatus.CREATED, order.getStatus());
            verify(stateService, never()).processEvent(OrderStatus.CREATED, OrderEvent.PAY);
            verify(stateService, never()).processEvent(OrderStatus.CREATED, OrderEvent.CANCEL);
            verify(repository).save(order);
        }

        @Test
        void rejectsEventForDifferentPaymentOnceOrderHasPayment() {
            Order order = order(OrderStatus.CREATED, 35L);
            when(repository.findByClientIdAndId(20L, 10L)).thenReturn(Optional.of(order));

            assertThrows(IllegalStateException.class,
                    () -> service.processPaymentResult(20L, 10L, 36L, PaymentStatus.APPROVED));

            verify(repository, never()).save(order);
            verify(stateService, never()).processEvent(OrderStatus.CREATED, OrderEvent.PAY);
        }

        @Test
        void rejectsNullPaymentIdBeforeLoadingOrder() {
            assertThrows(NullPointerException.class,
                    () -> service.processPaymentResult(20L, 10L, null, PaymentStatus.PENDING));
            verify(repository, never()).findByClientIdAndId(20L, 10L);
        }

        @Test
        void rejectsNullPaymentStatusBeforeLoadingOrder() {
            assertThrows(NullPointerException.class,
                    () -> service.processPaymentResult(20L, 10L, 35L, null));
            verify(repository, never()).findByClientIdAndId(20L, 10L);
        }
    }

    private Order order(OrderStatus status, Long paymentId) {
        Order order = new Order();
        order.setId(10L);
        order.setClientId(20L);
        order.setPaymentId(paymentId);
        order.setStatus(status);
        return order;
    }
}
