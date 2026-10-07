package io.wlailson.github.e_commerce_order_service.dto.response;

import io.wlailson.github.e_commerce_order_service.domain.Order;
import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record OrderResponseDTO(
        Long id,
        Long paymentId,
        Instant moment,
        OrderStatus status,
        Set<OrderItemResponseDTO> items
) {

    public OrderResponseDTO(Order entity) {
        this(
                entity.getId(),
                entity.getPaymentId(),
                entity.getMoment(),
                entity.getStatus(),
                entity.getItems().stream().map(OrderItemResponseDTO::new)
                        .collect(Collectors.toSet())
        );
    }

    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(OrderItemResponseDTO::getTotalItemPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
