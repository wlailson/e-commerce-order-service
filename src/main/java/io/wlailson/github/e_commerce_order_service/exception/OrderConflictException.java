package io.wlailson.github.e_commerce_order_service.exception;

public class OrderConflictException extends IllegalStateException {

    public OrderConflictException(String message) {
        super(message);
    }
}
