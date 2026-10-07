package io.wlailson.github.e_commerce_order_service.config;

import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;
import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import static io.wlailson.github.e_commerce_order_service.domain.OrderStatus.*;

@Configuration
@EnableStateMachineFactory
public class OrderStateMachineConfig extends StateMachineConfigurerAdapter<OrderStatus, OrderEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<OrderStatus, OrderEvent> states) throws Exception {
        states
                .withStates()
                .initial(CREATED)
                .state(PAID)
                .state(SHIPPED)
                .state(DELIVERED)
                .state(CANCELLED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<OrderStatus, OrderEvent> transitions) throws Exception {
        transitions
                .withExternal()
                .source(CREATED).target(PAID).event(OrderEvent.PAY)
                .and()

                .withExternal()
                .source(PAID).target(SHIPPED).event(OrderEvent.SHIP)
                .and()

                .withExternal()
                .source(SHIPPED).target(DELIVERED).event(OrderEvent.DELIVER)
                .and()

                .withExternal()
                .source(CREATED).target(CANCELLED).event(OrderEvent.CANCEL);
    }
}
