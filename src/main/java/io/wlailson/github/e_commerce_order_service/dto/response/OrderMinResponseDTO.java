package io.wlailson.github.e_commerce_order_service.dto.response;

import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.projections.OrderMinResponseProjection;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(name = "PedidoResumoResponse", description = "Resumo de um pedido na listagem.")
public record OrderMinResponseDTO(
        @Schema(description = "Identificador do pedido.", example = "31")
        Long id,
        @Schema(description = "Data e hora de criação do pedido em UTC.", example = "2025-01-01T10:00:00Z")
        Instant moment,
        @Schema(description = "Estado atual do pedido.", example = "PAID")
        OrderStatus status,
        @Schema(description = "Valor total do pedido.", example = "15.00")
        BigDecimal total
) {

    public OrderMinResponseDTO(OrderMinResponseProjection projection) {
        this(
                projection.getId(),
                projection.getMoment(),
                projection.getStatus(),
                projection.getTotal()
        );
    }
}
