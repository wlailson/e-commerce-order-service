package io.wlailson.github.e_commerce_order_service.dto.response;

import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.projections.OrderMinResponseProjection;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderMinResponseDTO(
        Long id,
        Instant moment,
        OrderStatus status,
        BigDecimal total
) {

    public OrderMinResponseDTO(OrderMinResponseProjection projection) {
        this(
                projection.getId(),
                projection.getMoment(),
                projection.getStatus(),
                projection.getTotal()
        );
    }
}
