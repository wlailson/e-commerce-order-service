package io.wlailson.github.e_commerce_order_service.message;

import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;

import java.time.Instant;
import java.util.Set;

public record OrderUpdatedMessage(
        Long orderId,
        OrderEvent event,
        Instant occurredAt
) {
}
