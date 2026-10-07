package io.wlailson.github.e_commerce_order_service.dto.request;

import java.math.BigDecimal;

public record OrderItemRequestDTO(
        Long productId,
        Integer quantity,
        BigDecimal price
) {
}