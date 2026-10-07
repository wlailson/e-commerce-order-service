package io.wlailson.github.e_commerce_order_service.service;

import io.wlailson.github.e_commerce_order_service.domain.Order;
import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;
import io.wlailson.github.e_commerce_order_service.domain.OrderItem;
import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.dto.request.OrderRequestDTO;
import io.wlailson.github.e_commerce_order_service.dto.request.OrderUpdateRequest;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderMinResponseDTO;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderResponseDTO;
import io.wlailson.github.e_commerce_order_service.exception.OrderConflictException;
import io.wlailson.github.e_commerce_order_service.message.KafkaProducerTopics;
import io.wlailson.github.e_commerce_order_service.message.OrderCreateMessage;
import io.wlailson.github.e_commerce_order_service.message.OrderItemCreateMessage;
import io.wlailson.github.e_commerce_order_service.message.OrderUpdatedMessage;
import io.wlailson.github.e_commerce_order_service.message.PaymentStatus;
import io.wlailson.github.e_commerce_order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.wlailson.github.e_commerce_order_service.domain.OrderStatus.CREATED;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final OrderStateService stateService;
    private final KafkaProducerService kafkaProducerService;
    private final KafkaProducerTopics kafkaProducerTopics;

    @Transactional(readOnly = true)
    public OrderResponseDTO findOrderById(Long clientId, Long orderId) {
        Order order = loadEntity(clientId, orderId);
        return new OrderResponseDTO(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderMinResponseDTO> findAllOrdersByStatus(Pageable pageable, Long clientId, OrderStatus status) {
        return repository.searchAllOrdersByClientIdAndStatus(pageable, clientId, status.name())
                .map(OrderMinResponseDTO::new);
    }

    @Transactional
    public OrderResponseDTO insertOrder(Long clientId, OrderRequestDTO request) {
        Instant createdAt = Instant.now();
        Instant expiresAt = createdAt.plus(1, ChronoUnit.DAYS);
        Order order = createOrder(clientId, createdAt);
        Set<OrderItem> items = createOrderItems(order, request);
        order.getItems().addAll(items);

        repository.save(order);
        kafkaProducerService.send(
                kafkaProducerTopics.orderCreated(),
                createOrderCreateMessage(order, createdAt, expiresAt, items)
        );

        return new OrderResponseDTO(order);
    }

    @Transactional
    public OrderResponseDTO updateOrder(Long userId, Long orderId, OrderUpdateRequest request) {
        Order entity = loadEntity(userId, orderId);
        OrderStatus orderStatus = stateService.processEvent(entity.getStatus(), request.event());
        entity.setStatus(orderStatus);
        OrderResponseDTO response = new OrderResponseDTO(entity);

        kafkaProducerService.send(
                kafkaProducerTopics.orderUpdated(),
                new OrderUpdatedMessage(
                        orderId,
                        request.event(),
                        Instant.now()
                ));

        return response;
    }

    Order loadEntity(Long userId, Long orderId) {
        return repository.findByClientIdAndId(userId, orderId).orElseThrow();
    }

    @Transactional
    public void processPaymentResult(Long userId, Long orderId, Long paymentId, PaymentStatus paymentStatus) {
        Objects.requireNonNull(paymentId, "paymentId must not be null");
        Objects.requireNonNull(paymentStatus, "paymentStatus must not be null");

        Order entity = loadEntity(userId, orderId);
        if (entity.getPaymentId() != null && !entity.getPaymentId().equals(paymentId)) {
            throw new OrderConflictException(
                    "Order " + orderId + " is already associated with payment " + entity.getPaymentId());
        }

        OrderEvent event = switch (paymentStatus) {
            case APPROVED -> OrderEvent.PAY;
            case REFUSED -> OrderEvent.CANCEL;
            case PENDING, CANCELLED, REFUNDED -> null;
        };
        if (event != null) {
            OrderStatus targetStatus = event == OrderEvent.PAY ? OrderStatus.PAID : OrderStatus.CANCELLED;
            if (entity.getStatus() != targetStatus) {
                entity.setStatus(stateService.processEvent(entity.getStatus(), event));
            }
        }

        entity.setPaymentId(paymentId);
        repository.save(entity);
    }

    private Order createOrder(Long clientId, Instant createdAt) {
        Order order = new Order();
        order.setClientId(clientId);
        order.setMoment(createdAt);
        order.setStatus(CREATED);
        return order;
    }

    private Set<OrderItem> createOrderItems(Order order, OrderRequestDTO request) {
        return request.items().stream()
                .map(itemRequest -> {
                    OrderItem item = new OrderItem();
                    item.setOrder(order);
                    item.setPrice(itemRequest.price());
                    item.setProductId(itemRequest.productId());
                    item.setQuantity(itemRequest.quantity());
                    return item;
                })
                .collect(Collectors.toSet());
    }

    private OrderCreateMessage createOrderCreateMessage(
            Order order,
            Instant createdAt,
            Instant expiresAt,
            Set<OrderItem> items) {
        return new OrderCreateMessage(
                order.getId(),
                OrderEvent.CREATE,
                createdAt,
                expiresAt,
                items.stream()
                        .map(item -> new OrderItemCreateMessage(
                                item.getProductId(),
                                item.getQuantity(),
                                item.getPrice()
                        ))
                        .collect(Collectors.toSet())
        );
    }
}
