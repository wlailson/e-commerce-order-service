package io.wlailson.github.e_commerce_order_service.handlers;

import io.wlailson.github.e_commerce_order_service.exception.OrderConflictException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsEnglishValidationProblemDetailWithFieldErrors() {
        var bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "items", "must not be empty"));
        var exception = new MethodArgumentNotValidException(null, bindingResult);

        ProblemDetail problem = handler.handleValidationException(exception);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("One or more fields are invalid.", problem.getDetail());
        assertEquals(Map.of("items", "must not be empty"), problem.getProperties().get("errors"));
    }

    @Test
    void mapsMissingResourceToNotFound() {
        ProblemDetail problem = handler.handleNotFoundException(new NoSuchElementException());

        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
        assertEquals("The requested resource was not found.", problem.getDetail());
    }

    @Test
    void mapsOrderConflictToConflict() {
        ProblemDetail problem = handler.handleOrderConflictException(
                new OrderConflictException("Invalid order transition"));

        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals("Invalid order transition", problem.getDetail());
    }

    @Test
    void hidesUnexpectedExceptionMessageInInternalServerErrorResponse() {
        ProblemDetail problem = handler.handleUnexpectedException(
                new IllegalStateException("database credentials leaked"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
        assertEquals("An unexpected error occurred.", problem.getDetail());
        assertFalse(problem.getDetail().contains("database credentials"));
    }
}
