package io.wlailson.github.e_commerce_order_service;

import io.wlailson.github.e_commerce_order_service.config.OrderStateMachineConfig;
import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;
import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.service.OrderStateService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig(classes = {OrderStateMachineConfig.class, OrderStateService.class})
class OrderStateServiceTests {

    @Autowired
    private OrderStateService service;

    @Nested
    class ProcessEvent {

        @Test
        void processesValidTransitions() {
            assertEquals(OrderStatus.PAID, service.processEvent(OrderStatus.CREATED, OrderEvent.PAY));
            assertEquals(OrderStatus.SHIPPED, service.processEvent(OrderStatus.PAID, OrderEvent.SHIP));
            assertEquals(OrderStatus.DELIVERED, service.processEvent(OrderStatus.SHIPPED, OrderEvent.DELIVER));
            assertEquals(OrderStatus.CANCELLED, service.processEvent(OrderStatus.CREATED, OrderEvent.CANCEL));
        }

        @Test
        void rejectsInvalidTransitionsAndTerminalStateEvents() {
            assertThrows(IllegalStateException.class,
                    () -> service.processEvent(OrderStatus.CREATED, OrderEvent.SHIP));
            assertThrows(IllegalStateException.class,
                    () -> service.processEvent(OrderStatus.DELIVERED, OrderEvent.PAY));
            assertThrows(IllegalStateException.class,
                    () -> service.processEvent(OrderStatus.CANCELLED, OrderEvent.PAY));
            assertThrows(IllegalStateException.class,
                    () -> service.processEvent(OrderStatus.CREATED, OrderEvent.CREATE));
        }

        @Test
        void rejectsNullStatusOrEvent() {
            assertThrows(NullPointerException.class,
                    () -> service.processEvent(null, OrderEvent.PAY));
            assertThrows(NullPointerException.class,
                    () -> service.processEvent(OrderStatus.CREATED, null));
        }

        @Test
        void processesConcurrentOrdersWithIndependentMachines() throws Exception {
            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                var pay = executor.submit(() -> service.processEvent(OrderStatus.CREATED, OrderEvent.PAY));
                var ship = executor.submit(() -> service.processEvent(OrderStatus.PAID, OrderEvent.SHIP));

                assertEquals(OrderStatus.PAID, pay.get());
                assertEquals(OrderStatus.SHIPPED, ship.get());
            }
        }
    }
}
