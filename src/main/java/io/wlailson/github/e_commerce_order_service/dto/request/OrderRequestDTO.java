package io.wlailson.github.e_commerce_order_service.dto.request;

import java.util.Set;

public record OrderRequestDTO(
        Set<OrderItemRequestDTO> items) {
}
