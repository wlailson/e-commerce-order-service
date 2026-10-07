package io.wlailson.github.e_commerce_order_service.controller;

import io.wlailson.github.e_commerce_order_service.domain.OrderStatus;
import io.wlailson.github.e_commerce_order_service.dto.request.OrderRequestDTO;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderMinResponseDTO;
import io.wlailson.github.e_commerce_order_service.dto.response.OrderResponseDTO;
import io.wlailson.github.e_commerce_order_service.service.AuthenticatedUser;
import io.wlailson.github.e_commerce_order_service.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class OrderControllerIntegrationTest {

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private AuthenticatedUser authenticatedUser;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(authenticatedUser.getUserId()).thenReturn(77L);
    }

    @Nested
    class FindAllOrdersByStatus {

        @Test
        void returnsPagedOrdersUsingDefaultPaidStatusAndAuthenticatedClient() throws Exception {
            var response = new OrderMinResponseDTO(
                    31L,
                    Instant.parse("2025-01-01T10:00:00Z"),
                    OrderStatus.PAID,
                    new BigDecimal("15.00"));
            when(orderService.findAllOrdersByStatus(PageRequest.of(0, 20), 77L, OrderStatus.PAID))
                    .thenReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));

            mockMvc.perform(get("/orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(31))
                    .andExpect(jsonPath("$.content[0].status").value("PAID"))
                    .andExpect(jsonPath("$.content[0].total").value(15.00))
                    .andExpect(jsonPath("$.totalElements").value(1));

            verify(orderService).findAllOrdersByStatus(PageRequest.of(0, 20), 77L, OrderStatus.PAID);
        }

        @Test
        void honorsStatusAndPageParameters() throws Exception {
            var pageable = PageRequest.of(1, 5);
            when(orderService.findAllOrdersByStatus(pageable, 77L, OrderStatus.SHIPPED))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));

            mockMvc.perform(get("/orders")
                            .param("status", "SHIPPED")
                            .param("page", "1")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.size").value(5));

            verify(orderService).findAllOrdersByStatus(pageable, 77L, OrderStatus.SHIPPED);
        }
    }

    @Nested
    class FindOrderById {

        @Test
        void returnsOrderDetailsForAuthenticatedClient() throws Exception {
            var response = new OrderResponseDTO(31L, 901L,
                    Instant.parse("2025-01-01T10:00:00Z"), OrderStatus.PAID, Set.of());
            when(orderService.findOrderById(77L, 31L)).thenReturn(response);

            mockMvc.perform(get("/orders/31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(31))
                    .andExpect(jsonPath("$.paymentId").value(901))
                    .andExpect(jsonPath("$.status").value("PAID"));

            verify(orderService).findOrderById(77L, 31L);
        }
    }

    @Nested
    class InsertOrder {

        @Test
        void createsOrderAndReturnsLocationHeader() throws Exception {
            var response = new OrderResponseDTO(31L, null,
                    Instant.parse("2025-01-01T10:00:00Z"), OrderStatus.CREATED, Set.of());
            when(orderService.insertOrder(eq(77L), any(OrderRequestDTO.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/orders")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"items":[{"productId":900,"quantity":3,"price":5.00}]}
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/orders/31"))
                    .andExpect(jsonPath("$.id").value(31))
                    .andExpect(jsonPath("$.status").value("CREATED"));

            verify(orderService).insertOrder(eq(77L), any(OrderRequestDTO.class));
            verify(authenticatedUser).getUserId();
        }

        @Test
        void rejectsOrderWithoutItems() throws Exception {
            mockMvc.perform(post("/orders")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"items":[]}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.errors.items").exists());
        }

        @Test
        void rejectsInvalidOrderItemFields() throws Exception {
            mockMvc.perform(post("/orders")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {"items":[{"productId":0,"quantity":0,"price":-1.00}]}
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation failed"))
                    .andExpect(jsonPath("$.errors").isNotEmpty());
        }
    }
}
