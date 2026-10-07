package io.wlailson.github.e_commerce_order_service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.dto.request.OrderItemRequestDTO;
import io.wlailson.github.e_commerce_order_service.dto.request.OrderRequestDTO;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderResponseDTO;
import io.wlailson.github.e_commerce_order_service.message.PaymentCreatedMessage;
import io.wlailson.github.e_commerce_order_service.message.PaymentStatus;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import java.math.BigDecimal;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ITOrderContainerIntegrationTest {

    private static final KeyPair TEST_KEY_PAIR = generateTestKeyPair();
    private static final String ORDER_CREATED_TOPIC = "order-created-event";
    private static final String PAYMENT_CREATED_TOPIC = "payment-created-event";
    private static final long CLIENT_ID = 77L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PostgreSQLContainer postgresContainer;

    @Autowired
    private KafkaContainer kafkaContainer;

    @Autowired
    private RabbitMQContainer rabbitContainer;

    @Autowired
    private ConnectionFactory rabbitConnectionFactory;

    @Autowired
    private RestTestClient client;

    private final JwtEncoder jwtEncoder = createJwtEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @DynamicPropertySource
    static void registerTestProperties(DynamicPropertyRegistry registry) {
        registry.add("jwt.public-key", ITOrderContainerIntegrationTest::encodedPublicKey);
        registry.add("spring.kafka.producer.topics.orderCreated", () -> ORDER_CREATED_TOPIC);
        registry.add("spring.kafka.consumer.topics.paymentCreated", () -> PAYMENT_CREATED_TOPIC);
    }

    @Nested
    class ContainerLifecycle {

        @Test
        void startsAllConfiguredContainers() {
            assertThat(postgresContainer.isRunning()).isTrue();
            assertThat(kafkaContainer.isRunning()).isTrue();
            assertThat(rabbitContainer.isRunning()).isTrue();
        }
    }

    @Nested
    class PostgreSQLContainerConnection {

        @Test
        void connectsToDatabaseAndRunsQuery() {
            assertThat(jdbcTemplate.queryForObject("select 1", Integer.class)).isEqualTo(1);
        }
    }

    @Nested
    class KafkaContainerConnection {

        @Test
        void exposesKafkaBrokerMetadata() throws Exception {
            try (Admin admin = Admin.create(Map.of(
                    AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                    kafkaContainer.getBootstrapServers()))) {
                assertThat(admin.describeCluster().nodes().get(10, TimeUnit.SECONDS)).isNotEmpty();
            }
        }
    }

    @Nested
    class RabbitMQContainerMessaging {

        @Test
        void acceptsAmqpConnection() {
            try (var connection = rabbitConnectionFactory.createConnection()) {
                assertThat(connection.isOpen()).isTrue();
            }
        }

        @Test
        void publishesAndReceivesMessageThroughRabbitMq() {
            RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitConnectionFactory);
            Queue queue = new Queue("order-service-it-" + UUID.randomUUID(), false, false, true);
            rabbitAdmin.declareQueue(queue);
            RabbitTemplate rabbitTemplate = new RabbitTemplate(rabbitConnectionFactory);
            try {
                rabbitTemplate.convertAndSend(queue.getName(), "integration-message");

                assertThat(rabbitTemplate.receiveAndConvert(queue.getName(), 5_000L))
                        .isEqualTo("integration-message");
            } finally {
                rabbitAdmin.deleteQueue(queue.getName());
            }
        }
    }

    @Nested
    class OrderHttpAndKafkaFlow {

        @Test
        void createsPersistsPublishesAndReadsOrder() throws Exception {
            var createdOrder = createOrder(CLIENT_ID);
            assertThat(createdOrder.id()).isNotNull();
            assertThat(createdOrder.status()).isEqualTo(OrderStatus.CREATED);
            assertThat(createdOrder.items()).hasSize(1);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from tb_order where id = ? and client_id = ?",
                    Integer.class, new Object[]{createdOrder.id(), CLIENT_ID})).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject(
                    "select count(*) from tb_order_item where order_id = ?",
                    Integer.class, new Object[]{createdOrder.id()})).isEqualTo(1);

            JsonNode publishedEvent = awaitOrderCreatedEvent(createdOrder.id());
            assertThat(publishedEvent.path("orderId").asLong()).isEqualTo(createdOrder.id());
            assertThat(publishedEvent.path("event").asText()).isEqualTo("CREATE");
            assertThat(publishedEvent.path("items")).hasSize(1);

            client.get().uri("/orders/{orderId}", createdOrder.id())
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(CLIENT_ID))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(createdOrder.id())
                    .jsonPath("$.status").isEqualTo("CREATED")
                    .jsonPath("$.items[0].productId").isEqualTo(900)
                    .jsonPath("$.totalPrice").isEqualTo(15.0);
        }

        @Test
        void refusesUnauthenticatedRequests() {
            client.get().uri("/orders")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }
    }

    @Nested
    class PaymentKafkaFlow {

        @Test
        void consumesApprovedPaymentAndPersistsPaidOrder() throws Exception {
            var createdOrder = createOrder(CLIENT_ID);
            PaymentCreatedMessage payment = new PaymentCreatedMessage(
                    3501L,
                    createdOrder.id(),
                    CLIENT_ID,
                    new BigDecimal("15.00"),
                    PaymentStatus.APPROVED,
                    LocalDateTime.now(),
                    LocalDateTime.now());
            publishPayment(payment);

            org.awaitility.Awaitility.await()
                    .atMost(Duration.ofSeconds(15))
                    .pollInterval(Duration.ofMillis(100))
                    .untilAsserted(() -> {
                        assertThat(jdbcTemplate.queryForObject(
                                "select status from tb_order where id = ?",
                                String.class, new Object[]{createdOrder.id()})).isEqualTo("PAID");
                        assertThat(jdbcTemplate.queryForObject(
                                "select payment_id from tb_order where id = ?",
                                Long.class, new Object[]{createdOrder.id()})).isEqualTo(3501L);
                    });

            client.get().uri("/orders/{orderId}", createdOrder.id())
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(CLIENT_ID))
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("PAID")
                    .jsonPath("$.paymentId").isEqualTo(3501);
        }
    }

    private OrderResponseDTO createOrder(long clientId) {
        OrderRequestDTO request = new OrderRequestDTO(Set.of(
                new OrderItemRequestDTO(900L, 3, new BigDecimal("5.00"))));
        var result = client.post().uri("/orders")
                .header(HttpHeaders.AUTHORIZATION, bearerToken(clientId))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists(HttpHeaders.LOCATION)
                .expectBody(OrderResponseDTO.class)
                .returnResult();

        URI location = result.getResponseHeaders().getLocation();
        assertThat(location).isNotNull();
        assertThat(location.getPath()).startsWith("/orders/");
        return java.util.Objects.requireNonNull(result.getResponseBody());
    }

    private JsonNode awaitOrderCreatedEvent(Long orderId) throws Exception {
        Properties properties = consumerProperties("order-created-it-" + orderId);
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties)) {
            consumer.subscribe(Set.of(ORDER_CREATED_TOPIC));
            long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
            while (System.nanoTime() < deadline) {
                var records = consumer.poll(Duration.ofMillis(250));
                for (var record : records) {
                    JsonNode event = objectMapper.readTree(record.value());
                    if (event.path("orderId").asLong() == orderId) {
                        return event;
                    }
                }
            }
        }
        throw new AssertionError("No order-created event was received for order " + orderId);
    }

    private void publishPayment(PaymentCreatedMessage payment) throws Exception {
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        String paymentJson = objectMapper.writeValueAsString(Map.of(
                "id", payment.id(),
                "orderId", payment.orderId(),
                "userId", payment.userId(),
                "price", payment.price(),
                "status", payment.status().name(),
                "created", payment.created().toString(),
                "updated", payment.updated().toString()));
        try (KafkaProducer<String, String> producer = new KafkaProducer<>(properties)) {
            producer.send(new ProducerRecord<>(
                    PAYMENT_CREATED_TOPIC, payment.orderId().toString(), paymentJson))
                    .get(10, TimeUnit.SECONDS);
        }
    }

    private Properties consumerProperties(String groupId) {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return properties;
    }

    private String bearerToken(long clientId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("order-service-integration-test")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("userId", clientId)
                .claim("roles", Set.of())
                .build();
        return "Bearer " + jwtEncoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(SignatureAlgorithm.RS256).build(),
                claims)).getTokenValue();
    }

    private static JwtEncoder createJwtEncoder() {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) TEST_KEY_PAIR.getPublic())
                .privateKey((RSAPrivateKey) TEST_KEY_PAIR.getPrivate())
                .keyID("order-service-test")
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(rsaKey)));
    }

    private static KeyPair generateTestKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to generate test JWT key pair", exception);
        }
    }

    private static String encodedPublicKey() {
        return Base64.getEncoder().encodeToString(TEST_KEY_PAIR.getPublic().getEncoded());
    }

}
