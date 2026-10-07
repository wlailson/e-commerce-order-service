package io.wlailson.github.e_commerce_order_service.message;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.kafka.consumer.topics")
public record KafkaConsumerTopics(
        String paymentCreated,
        String paymentUpdated
) {
}
