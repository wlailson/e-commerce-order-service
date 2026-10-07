package io.wlailson.github.e_commerce_order_service.message;

import java.math.BigDecimal;

public record OrderItemCreateMessage(Long productId, Integer quantity, BigDecimal price) {
}
