package io.wlailson.github.e_commerce_order_service.projections;

import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public interface OrderMinResponseProjection {

    Long getId();
    Instant getMoment();
    OrderStatus getStatus();
    BigDecimal getTotal();
}
