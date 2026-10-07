package io.wlailson.github.e_commerce_order_service.controller;

import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.dto.request.OrderRequestDTO;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderMinResponseDTO;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderResponseDTO;
import io.wlailson.github.e_commerce_order_service.service.AuthenticatedUser;
import io.wlailson.github.e_commerce_order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final AuthenticatedUser authenticatedUser;

    @GetMapping
    public ResponseEntity<Page<OrderMinResponseDTO>> findAllOrdersByStatus(
            @RequestParam(defaultValue = "PAID", name = "status") OrderStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(
                orderService.findAllOrdersByStatus(pageable, authenticatedUser.getUserId(), status));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> findOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.findOrderById(authenticatedUser.getUserId(), orderId));
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> insertOrder(@RequestBody OrderRequestDTO request) {
        OrderResponseDTO response = orderService.insertOrder(authenticatedUser.getUserId(), request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
