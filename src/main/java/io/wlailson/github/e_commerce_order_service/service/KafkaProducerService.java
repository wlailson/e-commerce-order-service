package io.wlailson.github.e_commerce_order_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(String topic, Object message) {
        kafkaTemplate.send(topic, message)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        log.error(
                                "Erro ao enviar mensagem para o tópico {}",
                                topic,
                                exception
                        );
                        return;
                    }
                    log.info(
                            "Mensagem enviada com sucesso. Topic: {}, Partition: {}, Offset: {}",
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset()
                    );
                });
    }
}
