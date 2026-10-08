# E-commerce Order Service

Serviço responsável pela criação e consulta de pedidos e pelo controle de seus estados. Publica eventos de criação/atualização e processa eventos de resultado de pagamento recebidos pelo Kafka.

## Tecnologias

- Java 25, Maven e Spring Boot 4.1.1.
- Spring MVC, validação, Spring Security e OAuth2 Resource Server para validação JWT.
- Spring Data JPA, PostgreSQL e Flyway.
- Apache Kafka para publicação e consumo de eventos.
- Spring StateMachine para controle de transições do pedido.
- Testes com JUnit Jupiter, Mockito e Testcontainers para PostgreSQL e Kafka.

## Executar localmente

Pré-requisitos: JDK 25, PostgreSQL e Kafka acessíveis. O perfil de desenvolvimento é ativado por padrão; a configuração local do banco está em `src/main/resources/application-dev.yaml`.

```bash
./mvnw spring-boot:run
```

Variáveis de configuração principais:

| Variável | Uso |
|---|---|
| `SERVER_PORT` | Porta HTTP (padrão `8080`). |
| `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Conexão com PostgreSQL. |
| `KAFKA_BOOTSTRAP_SERVERS` | Endereço do cluster Kafka (padrão local `localhost:9092`). |
| `KAFKA_TOPIC_CREATED`, `KAFKA_TOPIC_UPDATED`, `KAFKA_TOPIC_PAYMENT_CREATED` | Tópicos usados para eventos de pedidos e pagamentos. |
| `JWT_PUBLIC_KEY` | Chave pública para validação dos tokens. |

Flyway gerencia as migrações do banco. As configurações de chaves e credenciais de desenvolvimento não devem ser reutilizadas em produção.

## Testes

```bash
./mvnw test
./mvnw verify
```

`verify` executa os testes de integração configurados com Maven Failsafe. Docker deve estar disponível para os testes que usam Testcontainers.

## API e eventos

Os endpoints de pedidos estão sob `/orders`. Consulte a documentação HTTP gerada pela aplicação:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

O serviço publica eventos `order-created-event` e `order-updated-event` e consome `payment-created-event` por padrão. Esses nomes são configuráveis por variáveis de ambiente; os contratos detalhados estão documentados no Swagger e nos modelos do serviço.

## Projeto

Veja a arquitetura e os demais serviços no [README central do BFF](https://github.com/wlailson/e-commerce-BFF-service#readme).
