package io.wlailson.github.e_commerce_order_service.service;

import io.wlailson.github.e_commerce_order_service.message.PaymentCreatedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumerService {

    private final OrderService orderService;

    @KafkaListener(topics = "${spring.kafka.consumer.topics.paymentCreated}")
    public void paymentCreated(PaymentCreatedMessage message) {
        log.info("Received payment-created event: payment={}", message);
        orderService.processPaymentResult(
                message.userId(),
                message.orderId(),
                message.id(),
                message.status()
        );
        log.info("Order {} updated with payment {} status {}", message.orderId(), message.id(), message.status());
    }
}