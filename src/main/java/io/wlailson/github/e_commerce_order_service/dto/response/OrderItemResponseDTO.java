package io.wlailson.github.e_commerce_order_service.dto.response;

import io.wlailson.github.e_commerce_order_service.domain.OrderItem;

import java.math.BigDecimal;

public record OrderItemResponseDTO(
        Long id,
        Integer quantity,
        BigDecimal price,
        Long productId) {

    public OrderItemResponseDTO(OrderItem entity) {
        this(
                entity.getId(),
                entity.getQuantity(),
                entity.getPrice(),
                entity.getProductId()
        );
    }

    public BigDecimal getTotalItemPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
