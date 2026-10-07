package io.wlailson.github.e_commerce_order_service.service;

import io.wlailson.github.e_commerce_order_service.domain.Order;
import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;
import io.wlailson.github.e_commerce_order_service.domain.OrderItem;
import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.dto.request.OrderItemRequestDTO;
import io.wlailson.github.e_commerce_order_service.dto.request.OrderRequestDTO;
import io.wlailson.github.e_commerce_order_service.dto.request.OrderUpdateRequest;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderMinResponseDTO;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderResponseDTO;
import io.wlailson.github.e_commerce_order_service.message.KafkaProducerTopics;
import io.wlailson.github.e_commerce_order_service.message.OrderCreateMessage;
import io.wlailson.github.e_commerce_order_service.message.OrderUpdatedMessage;
import io.wlailson.github.e_commerce_order_service.projections.OrderMinResponseProjection;
import io.wlailson.github.e_commerce_order_service.repository.OrderRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderStateService stateService = mock(OrderStateService.class);
    private final KafkaProducerService kafkaProducerService = mock(KafkaProducerService.class);
    private final KafkaProducerTopics kafkaProducerTopics =
            new KafkaProducerTopics("order-created", "order-updated");
    private final OrderService service = new OrderService(
            repository, stateService, kafkaProducerService, kafkaProducerTopics);

    @Nested
    class FindOrderById {

        @Test
        void returnsOrderDetailsForClient() {
            Order entity = order(41L, 12L, OrderStatus.PAID);
            when(repository.findByClientIdAndId(12L, 41L)).thenReturn(Optional.of(entity));

            OrderResponseDTO result = service.findOrderById(12L, 41L);

            assertThat(result.id()).isEqualTo(41L);
            assertThat(result.status()).isEqualTo(OrderStatus.PAID);
            assertThat(result.items()).hasSize(1);
            assertThat(result.getTotalPrice()).isEqualByComparingTo("15.00");
        }

        @Test
        void throwsWhenOrderDoesNotBelongToClientOrDoesNotExist() {
            when(repository.findByClientIdAndId(12L, 41L)).thenReturn(Optional.empty());

            assertThrows(java.util.NoSuchElementException.class,
                    () -> service.findOrderById(12L, 41L));
        }
    }

    @Nested
    class FindAllOrdersByStatus {

        @Test
        void mapsRepositoryPageToResponseDtos() {
            var pageable = PageRequest.of(1, 5);
            var projection = mock(OrderMinResponseProjection.class);
            when(projection.getId()).thenReturn(41L);
            when(projection.getMoment()).thenReturn(Instant.parse("2025-01-01T10:00:00Z"));
            when(projection.getStatus()).thenReturn(OrderStatus.PAID);
            when(projection.getTotal()).thenReturn(new BigDecimal("15.00"));
            Page<OrderMinResponseProjection> page =
                    new PageImpl<>(List.of(projection), pageable, 6);
            when(repository.searchAllOrdersByClientIdAndStatus(pageable, 12L, "PAID"))
                    .thenReturn(page);

            Page<OrderMinResponseDTO> result =
                    service.findAllOrdersByStatus(pageable, 12L, OrderStatus.PAID);

            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getTotalElements()).isEqualTo(6);
            assertThat(result.getContent()).containsExactly(
                    new OrderMinResponseDTO(41L, Instant.parse("2025-01-01T10:00:00Z"),
                            OrderStatus.PAID, new BigDecimal("15.00")));
        }
    }

    @Nested
    class InsertOrder {

        @Test
        void savesOrderAndPublishesCreationMessage() {
            when(repository.save(any(Order.class))).thenAnswer(invocation -> {
                Order saved = invocation.getArgument(0);
                saved.setId(41L);
                return saved;
            });
            var request = new OrderRequestDTO(Set.of(
                    new OrderItemRequestDTO(900L, 3, new BigDecimal("5.00"))));

            OrderResponseDTO result = service.insertOrder(12L, request);

            assertThat(result.id()).isEqualTo(41L);
            assertThat(result.status()).isEqualTo(OrderStatus.CREATED);
            assertThat(result.getTotalPrice()).isEqualByComparingTo("15.00");
            ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
            verify(repository).save(savedOrder.capture());
            assertThat(savedOrder.getValue().getClientId()).isEqualTo(12L);
            assertThat(savedOrder.getValue().getItems()).hasSize(1);

            ArgumentCaptor<OrderCreateMessage> message =
                    ArgumentCaptor.forClass(OrderCreateMessage.class);
            verify(kafkaProducerService).send(eq("order-created"), message.capture());
            assertThat(message.getValue().orderId()).isEqualTo(41L);
            assertThat(message.getValue().event()).isEqualTo(OrderEvent.CREATE);
            assertThat(message.getValue().expiresAt())
                    .isBetween(message.getValue().occurredAt().plus(23, ChronoUnit.HOURS),
                            message.getValue().occurredAt().plus(25, ChronoUnit.HOURS));
            assertThat(message.getValue().items()).hasSize(1);
            assertThat(message.getValue().getPrice()).isEqualByComparingTo("15.00");
        }
    }

    @Nested
    class UpdateOrder {

        @Test
        void updatesStatusAndPublishesOrderEvent() {
            Order entity = order(41L, 12L, OrderStatus.CREATED);
            when(repository.findByClientIdAndId(12L, 41L)).thenReturn(Optional.of(entity));
            when(stateService.processEvent(OrderStatus.CREATED, OrderEvent.PAY))
                    .thenReturn(OrderStatus.PAID);

            OrderResponseDTO result = service.updateOrder(
                    12L, 41L, new OrderUpdateRequest(OrderEvent.PAY));

            assertThat(result.status()).isEqualTo(OrderStatus.PAID);
            ArgumentCaptor<OrderUpdatedMessage> message =
                    ArgumentCaptor.forClass(OrderUpdatedMessage.class);
            verify(kafkaProducerService).send(eq("order-updated"), message.capture());
            assertThat(message.getValue().orderId()).isEqualTo(41L);
            assertThat(message.getValue().event()).isEqualTo(OrderEvent.PAY);
            assertThat(message.getValue().occurredAt()).isNotNull();
        }

        @Test
        void doesNotPublishWhenOrderCannotBeFoundForClient() {
            when(repository.findByClientIdAndId(12L, 41L)).thenReturn(Optional.empty());

            assertThrows(java.util.NoSuchElementException.class,
                    () -> service.updateOrder(12L, 41L, new OrderUpdateRequest(OrderEvent.PAY)));

            verify(kafkaProducerService, never()).send(any(), any());
        }

        @Test
        void doesNotPublishWhenStateTransitionIsInvalid() {
            Order entity = order(41L, 12L, OrderStatus.CREATED);
            when(repository.findByClientIdAndId(12L, 41L)).thenReturn(Optional.of(entity));
            when(stateService.processEvent(OrderStatus.CREATED, OrderEvent.SHIP))
                    .thenThrow(new IllegalStateException("Invalid transition"));

            assertThrows(IllegalStateException.class,
                    () -> service.updateOrder(12L, 41L, new OrderUpdateRequest(OrderEvent.SHIP)));

            verify(kafkaProducerService, never()).send(any(), any());
        }
    }

    private Order order(Long id, Long clientId, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setClientId(clientId);
        order.setMoment(Instant.parse("2025-01-01T10:00:00Z"));
        order.setStatus(status);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(900L);
        item.setQuantity(3);
        item.setPrice(new BigDecimal("5.00"));
        order.setItems(Set.of(item));
        return order;
    }
}
