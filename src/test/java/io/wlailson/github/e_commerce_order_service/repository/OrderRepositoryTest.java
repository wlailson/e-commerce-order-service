package io.wlailson.github.e_commerce_order_service.repository;

import io.wlailson.github.e_commerce_order_service.domain.Order;
import io.wlailson.github.e_commerce_order_service.domain.OrderItem;
import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.projections.OrderMinResponseProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16.0");

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Nested
    class FindByClientIdAndId {

        @Test
        void returnsOrderOwnedByClient() {
            Order savedOrder = orderRepository.saveAndFlush(order(12L, OrderStatus.PAID, 900L));

            assertThat(orderRepository.findByClientIdAndId(12L, savedOrder.getId()))
                    .contains(savedOrder);
        }

        @Test
        void doesNotReturnOrderOwnedByAnotherClient() {
            Order savedOrder = orderRepository.saveAndFlush(order(12L, OrderStatus.PAID, 900L));

            assertThat(orderRepository.findByClientIdAndId(13L, savedOrder.getId())).isEmpty();
        }

        @Test
        void returnsEmptyWhenOrderDoesNotExist() {
            assertThat(orderRepository.findByClientIdAndId(12L, 999L)).isEmpty();
        }
    }

    @Nested
    class SearchAllOrdersByClientIdAndStatus {

        @Test
        void filtersByClientAndStatusAndCalculatesItemTotals() {
            orderRepository.saveAndFlush(order(12L, OrderStatus.PAID, 900L));
            orderRepository.saveAndFlush(order(12L, OrderStatus.CREATED, 800L));
            orderRepository.saveAndFlush(order(13L, OrderStatus.PAID, 700L));

            Page<OrderMinResponseProjection> results = orderRepository.searchAllOrdersByClientIdAndStatus(
                    PageRequest.of(0, 10), 12L, "paid");

            assertThat(results.getTotalElements()).isEqualTo(1);
            assertThat(results.getContent()).singleElement().satisfies(projection -> {
                assertThat(projection.getStatus()).isEqualTo(OrderStatus.PAID);
                assertThat(projection.getTotal()).isEqualByComparingTo("15.00");
            });
        }

        @Test
        void returnsRequestedPageAndTotalCount() {
            orderRepository.saveAndFlush(order(12L, OrderStatus.PAID, 900L));
            orderRepository.saveAndFlush(order(12L, OrderStatus.PAID, 800L));
            orderRepository.saveAndFlush(order(12L, OrderStatus.PAID, 700L));

            Page<OrderMinResponseProjection> results = orderRepository.searchAllOrdersByClientIdAndStatus(
                    PageRequest.of(1, 2), 12L, "PAID");

            assertThat(results.getContent()).hasSize(1);
            assertThat(results.getNumber()).isEqualTo(1);
            assertThat(results.getTotalElements()).isEqualTo(3);
        }

        @Test
        void returnsEmptyPageWhenClientHasNoOrdersWithStatus() {
            orderRepository.saveAndFlush(order(12L, OrderStatus.CREATED, 900L));

            Page<OrderMinResponseProjection> results = orderRepository.searchAllOrdersByClientIdAndStatus(
                    PageRequest.of(0, 10), 12L, "PAID");

            assertThat(results.getContent()).isEmpty();
            assertThat(results.getTotalElements()).isZero();
        }
    }

    private Order order(Long clientId, OrderStatus status, Long productId) {
        Order order = new Order();
        order.setClientId(clientId);
        order.setMoment(Instant.parse("2025-01-01T10:00:00Z"));
        order.setStatus(status);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(productId);
        item.setQuantity(3);
        item.setPrice(new BigDecimal("5.00"));
        order.setItems(Set.of(item));
        return order;
    }
}
