package io.wlailson.github.e_commerce_order_service;

import io.wlailson.github.e_commerce_order_service.message.KafkaConsumerTopics;
import io.wlailson.github.e_commerce_order_service.message.KafkaProducerTopics;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({KafkaProducerTopics.class, KafkaConsumerTopics.class})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
