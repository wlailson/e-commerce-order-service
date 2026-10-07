package io.wlailson.github.e_commerce_order_service.message;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCreatedMessage(
        Long id,
        Long orderId,
        Long userId,
        BigDecimal price,
        PaymentStatus status,
        LocalDateTime created,
        LocalDateTime updated
) {
}
