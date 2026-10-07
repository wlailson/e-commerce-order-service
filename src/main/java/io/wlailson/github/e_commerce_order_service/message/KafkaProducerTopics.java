package io.wlailson.github.e_commerce_order_service.message;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.kafka.producer.topics")
public record KafkaProducerTopics(
        String orderCreated,
        String orderUpdated
) {
}
