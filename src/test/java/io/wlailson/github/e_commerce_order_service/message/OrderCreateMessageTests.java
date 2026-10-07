package io.wlailson.github.e_commerce_order_service.message;

import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderCreateMessageTests {

    @Test
    void calculatesTotalPriceFromUnitPricesAndQuantities() {
        OrderCreateMessage message = new OrderCreateMessage(
                1L,
                OrderEvent.CREATE,
                Instant.EPOCH,
                Instant.EPOCH.plusSeconds(60),
                Set.of(
                        new OrderItemCreateMessage(10L, 2, new BigDecimal("12.50")),
                        new OrderItemCreateMessage(20L, 3, new BigDecimal("4.25"))
                )
        );

        assertEquals(new BigDecimal("37.75"), message.getPrice());
    }
}
