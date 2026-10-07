package io.wlailson.github.e_commerce_order_service.dto.response;

import io.wlailson.github.e_commerce_order_service.domain.Order;
import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Schema(name = "PedidoResponse", description = "Detalhes completos de um pedido.")
public record OrderResponseDTO(
        @Schema(description = "Identificador do pedido.", example = "31")
        Long id,
        @Schema(description = "Identificador do pagamento associado, quando disponível.", example = "901", nullable = true)
        Long paymentId,
        @Schema(description = "Data e hora de criação do pedido em UTC.", example = "2025-01-01T10:00:00Z")
        Instant moment,
        @Schema(description = "Estado atual do pedido.", example = "PAID")
        OrderStatus status,
        @Schema(description = "Itens incluídos no pedido.")
        Set<OrderItemResponseDTO> items
) {

    public OrderResponseDTO(Order entity) {
        this(
                entity.getId(),
                entity.getPaymentId(),
                entity.getMoment(),
                entity.getStatus(),
                entity.getItems().stream().map(OrderItemResponseDTO::new)
                        .collect(Collectors.toSet())
        );
    }

    @Schema(description = "Valor total do pedido, calculado pela soma dos totais dos itens.",
            example = "15.00", accessMode = Schema.AccessMode.READ_ONLY)
    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(OrderItemResponseDTO::getTotalItemPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
