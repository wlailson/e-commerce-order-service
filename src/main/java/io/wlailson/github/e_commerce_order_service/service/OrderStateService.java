package io.wlailson.github.e_commerce_order_service.service;

import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;
import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.exception.OrderConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class OrderStateService {

    private final StateMachineFactory<OrderStatus, OrderEvent> stateMachineFactory;

    public OrderStatus processEvent(OrderStatus currentStatus, OrderEvent event) {
        Objects.requireNonNull(currentStatus, "currentStatus must not be null");
        Objects.requireNonNull(event, "event must not be null");

        StateMachine<OrderStatus, OrderEvent> stateMachine = stateMachineFactory.getStateMachine();
        try {
            DefaultStateMachineContext<OrderStatus, OrderEvent> context =
                    new DefaultStateMachineContext<>(currentStatus, null, null, null);
            Mono.when(stateMachine.getStateMachineAccessor().withAllRegions().stream()
                    .map(access -> access.resetStateMachineReactively(context))
                    .toList()).block();
            stateMachine.startReactively().block();

            List<StateMachineEventResult<OrderStatus, OrderEvent>> results = stateMachine.sendEventCollect(
                    Mono.just(MessageBuilder.withPayload(event).build())).block();
            if (results == null || results.isEmpty()) {
                throw invalidTransition(currentStatus, event);
            }

            results.forEach(result -> result.complete().block());
            if (results.stream().anyMatch(result ->
                    result.getResultType() != StateMachineEventResult.ResultType.ACCEPTED)) {
                throw invalidTransition(currentStatus, event);
            }

            if (stateMachine.getState() == null) {
                throw new IllegalStateException("State machine has no current state after event " + event);
            }
            return stateMachine.getState().getId();
        } finally {
            stateMachine.stopReactively().block();
        }
    }

    private OrderConflictException invalidTransition(OrderStatus currentStatus, OrderEvent event) {
        return new OrderConflictException("Invalid order transition: " + event + " from " + currentStatus);
    }
}
