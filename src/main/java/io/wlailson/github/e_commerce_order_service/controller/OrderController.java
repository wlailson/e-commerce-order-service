package io.wlailson.github.e_commerce_order_service.controller;

import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.dto.request.OrderRequestDTO;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderMinResponseDTO;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderResponseDTO;
import io.wlailson.github.e_commerce_order_service.service.AuthenticatedUser;
import io.wlailson.github.e_commerce_order_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Pedidos", description = "Consulta e criação de pedidos do cliente autenticado.")
@SecurityRequirement(name = "bearerAuth")
public class OrderController {

    private final OrderService orderService;
    private final AuthenticatedUser authenticatedUser;

    @GetMapping
    @Operation(summary = "Listar pedidos", description = "Lista os pedidos do cliente autenticado filtrando por status. A paginação usa os parâmetros page, size e sort.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de pedidos retornada com sucesso."),
            @ApiResponse(responseCode = "400", description = "Status ou parâmetros de paginação inválidos.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.")
    })
    public ResponseEntity<Page<OrderMinResponseDTO>> findAllOrdersByStatus(
            @Parameter(description = "Status pelo qual filtrar. Por padrão, retorna pedidos pagos.",
                    example = "PAID")
            @RequestParam(defaultValue = "PAID", name = "status") OrderStatus status,
            @ParameterObject Pageable pageable) {
        return ResponseEntity.ok(
                orderService.findAllOrdersByStatus(pageable, authenticatedUser.getUserId(), status));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Consultar pedido", description = "Retorna os detalhes de um pedido pertencente ao cliente autenticado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pedido encontrado."),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.")
    })
    public ResponseEntity<OrderResponseDTO> findOrderById(
            @Parameter(description = "Identificador do pedido.", example = "31")
            @PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.findOrderById(authenticatedUser.getUserId(), orderId));
    }

    @PostMapping
    @Operation(summary = "Criar pedido", description = "Cria um pedido para o cliente autenticado e publica o evento de criação.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pedido criado.",
                    content = @Content(schema = @Schema(implementation = OrderResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido.",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Token de autenticação ausente ou inválido.")
    })
    public ResponseEntity<OrderResponseDTO> insertOrder(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Itens que compõem o pedido.",
                    content = @Content(examples = @ExampleObject(
                            name = "Pedido com um item",
                            value = """
                                    {"items":[{"productId":900,"quantity":3,"price":5.00}]}
                                    """
                    ))
            )
            @Valid @RequestBody OrderRequestDTO request) {
        OrderResponseDTO response = orderService.insertOrder(authenticatedUser.getUserId(), request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }
}
