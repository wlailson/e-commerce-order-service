package io.wlailson.github.e_commerce_order_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(name = "ItemPedidoRequest", description = "Dados de um produto que será incluído no pedido.")
public record OrderItemRequestDTO(
        @Schema(description = "Identificador do produto no catálogo.", example = "900", minimum = "1")
        @NotNull @Positive
        Long productId,
        @Schema(description = "Quantidade solicitada.", example = "3", minimum = "1")
        @NotNull @Positive
        Integer quantity,
        @Schema(description = "Preço unitário do produto. Deve ser maior ou igual a zero.", example = "5.00", minimum = "0")
        @NotNull @DecimalMin("0.00")
        BigDecimal price
) {
}