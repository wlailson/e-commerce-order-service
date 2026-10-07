package io.wlailson.github.e_commerce_order_service.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

@Schema(name = "PedidoRequest", description = "Dados necessários para criar um pedido.")
public record OrderRequestDTO(
        @Schema(description = "Itens do pedido. Deve conter ao menos um item válido.")
        @NotEmpty
        Set<@NotNull @Valid OrderItemRequestDTO> items) {
}
