package io.wlailson.github.e_commerce_order_service.message;

import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

public record OrderCreateMessage(
        Long orderId,
        OrderEvent event,
        Instant occurredAt,
        Instant expiresAt,
        Set<OrderItemCreateMessage> items
) {
    public BigDecimal getPrice() {
        return items.stream()
                .map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
