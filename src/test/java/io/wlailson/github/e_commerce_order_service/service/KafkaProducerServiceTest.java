package io.wlailson.github.e_commerce_order_service.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KafkaProducerServiceTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private final KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
    private final KafkaProducerService service = new KafkaProducerService(kafkaTemplate);

    @Nested
    class Send {

        @Test
        void sendsMessageToRequestedTopic() {
            String topic = "order-created";
            Object message = new Object();
            when(kafkaTemplate.send(topic, message)).thenReturn(new CompletableFuture<>());

            service.send(topic, message);

            verify(kafkaTemplate).send(topic, message);
        }
    }
}
