package io.wlailson.github.e_commerce_order_service;

import io.wlailson.github.e_commerce_order_service.message.KafkaConsumerTopics;
import io.wlailson.github.e_commerce_order_service.message.KafkaProducerTopics;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({KafkaProducerTopics.class, KafkaConsumerTopics.class})
@OpenAPIDefinition(info = @Info(
		title = "E-commerce Order Service API",
		version = "1.0.0",
		description = "API para consulta e criação de pedidos do cliente autenticado. Os endpoints de negócio exigem um token JWT Bearer."
))
@SecurityScheme(
		name = "bearerAuth",
		type = SecuritySchemeType.HTTP,
		scheme = "bearer",
		bearerFormat = "JWT",
		description = "Token JWT de acesso."
)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
