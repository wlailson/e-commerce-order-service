package io.wlailson.github.e_commerce_order_service.domain;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento de domínio que provoca a transição de estado de um pedido.")
public enum OrderEvent {
    CREATE,
    PAY,
    SHIP,
    DELIVER,
    CANCEL
}
