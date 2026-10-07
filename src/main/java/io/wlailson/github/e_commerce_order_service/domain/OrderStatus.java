package io.wlailson.github.e_commerce_order_service.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Estado atual do pedido.")
public enum OrderStatus {
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;
}