package io.wlailson.github.e_commerce_order_service.dto.request;

import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;

public record OrderUpdateRequest(OrderEvent event) {
}
