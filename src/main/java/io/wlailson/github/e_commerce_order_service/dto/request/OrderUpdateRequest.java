package io.wlailson.github.e_commerce_order_service.dto.request;

import io.wlailson.github.e_commerce_order_service.domain.OrderEvent;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(name = "PedidoUpdateRequest", description = "Evento de domínio solicitado para alterar o estado de um pedido.")
public record OrderUpdateRequest(
        @Schema(description = "Evento que será aplicado ao pedido.", example = "PAY", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull OrderEvent event) {
}
