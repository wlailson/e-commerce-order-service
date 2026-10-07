package io.wlailson.github.e_commerce_order_service.dto.response;

import io.wlailson.github.e_commerce_order_service.domain.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "ItemPedidoResponse", description = "Item de um pedido retornado pela API.")
public record OrderItemResponseDTO(
        @Schema(description = "Identificador do item.", example = "101")
        Long id,
        @Schema(description = "Quantidade de unidades do produto.", example = "3")
        Integer quantity,
        @Schema(description = "Preço unitário do produto.", example = "5.00")
        BigDecimal price,
        @Schema(description = "Identificador do produto no catálogo.", example = "900")
        Long productId) {

    public OrderItemResponseDTO(OrderItem entity) {
        this(
                entity.getId(),
                entity.getQuantity(),
                entity.getPrice(),
                entity.getProductId()
        );
    }

    @Schema(description = "Preço total do item, calculado pela quantidade multiplicada pelo preço unitário.",
            example = "15.00", accessMode = Schema.AccessMode.READ_ONLY)
    public BigDecimal getTotalItemPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
