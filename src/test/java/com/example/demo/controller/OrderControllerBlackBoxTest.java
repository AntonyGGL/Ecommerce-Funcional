package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de Caja Negra — OrderController
 * Solo se verifica el contrato HTTP: status codes y campos del JSON.
 * No se examina la implementación interna del controlador ni del servicio.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderController — Tests de Caja Negra")
class OrderControllerBlackBoxTest {

    private MockMvc mockMvc;

    @Mock
    private OrderService orderService;

    @Mock
    @SuppressWarnings("unused") // requerido por @InjectMocks para inyectar en OrderController
    private UserService userService;

    @InjectMocks
    private OrderController orderController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Order testOrder;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(orderController).build();

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setShippingAddress("Av. Principal 456, Lima");
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setSubtotal(new BigDecimal("200.00"));
        testOrder.setTax(new BigDecimal("36.00"));
        testOrder.setTotal(new BigDecimal("236.00"));
    }

    // ─────────────────────────────────────────────────────
    // GET /api/orders
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/orders → 200 con lista completa de órdenes")
    void getAllOrders_returns200WithList() throws Exception {
        when(orderService.getAllOrders()).thenReturn(Arrays.asList(testOrder));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ─────────────────────────────────────────────────────
    // GET /api/orders/{id}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/orders/{id} existente → 200 con la orden")
    void getOrderById_found_returns200() throws Exception {
        when(orderService.getOrderById(1L)).thenReturn(testOrder);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shippingAddress").value("Av. Principal 456, Lima"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("GET /api/orders/{id} inexistente → 404")
    void getOrderById_notFound_returns404() throws Exception {
        when(orderService.getOrderById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────
    // GET /api/orders/user/{userId}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/orders/user/{userId} → 200 con órdenes del usuario")
    void getUserOrders_returns200() throws Exception {
        when(orderService.getUserOrders(1L)).thenReturn(Collections.singletonList(testOrder));

        mockMvc.perform(get("/api/orders/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    // ─────────────────────────────────────────────────────
    // PUT /api/orders/{id}/status
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/orders/{id}/status orden existente → 200 con estado actualizado")
    void updateOrderStatus_found_returns200() throws Exception {
        testOrder.setStatus(Order.OrderStatus.COMPLETED);
        when(orderService.updateOrderStatus(eq(1L), eq(Order.OrderStatus.COMPLETED))).thenReturn(testOrder);

        Map<String, String> payload = new HashMap<>();
        payload.put("status", "COMPLETED");

        mockMvc.perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/status orden inexistente → 404")
    void updateOrderStatus_notFound_returns404() throws Exception {
        when(orderService.updateOrderStatus(eq(99L), any())).thenReturn(null);

        Map<String, String> payload = new HashMap<>();
        payload.put("status", "COMPLETED");

        mockMvc.perform(put("/api/orders/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────
    // PUT /api/orders/{id}/confirm-delivery
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/orders/{id}/confirm-delivery existente → 200")
    void confirmDelivery_found_returns200() throws Exception {
        testOrder.setShippingStatus(Order.ShippingStatus.DELIVERED);
        when(orderService.updateShippingStatus(eq(1L), eq(Order.ShippingStatus.DELIVERED)))
                .thenReturn(testOrder);

        mockMvc.perform(put("/api/orders/1/confirm-delivery"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/orders/{id}/confirm-delivery inexistente → 404")
    void confirmDelivery_notFound_returns404() throws Exception {
        when(orderService.updateShippingStatus(eq(99L), eq(Order.ShippingStatus.DELIVERED)))
                .thenReturn(null);

        mockMvc.perform(put("/api/orders/99/confirm-delivery"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────
    // DELETE /api/orders/{id}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/orders/{id} → 204 sin contenido")
    void deleteOrder_returns204() throws Exception {
        doNothing().when(orderService).deleteOrder(1L);

        mockMvc.perform(delete("/api/orders/1"))
                .andExpect(status().isNoContent());
    }

    // ─────────────────────────────────────────────────────
    // POST /api/orders/cotizacion
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/orders/cotizacion válido → 200 con código de cotización")
    void createCotizacion_validRequest_returns200WithCode() throws Exception {
        com.example.demo.model.User user = new com.example.demo.model.User();
        user.setId(1L);

        Order cotizacion = new Order();
        cotizacion.setId(5L);
        cotizacion.setStatus(Order.OrderStatus.COTIZACION);
        cotizacion.setTotal(new BigDecimal("500.00"));
        cotizacion.setCotizacionCode("COT-2026-00005");

        when(userService.getUserById(1L)).thenReturn(user);
        when(orderService.createCotizacion(any(), any(), any(), any(), any())).thenReturn(cotizacion);

        Map<String, Object> item = new HashMap<>();
        item.put("productId", 1);
        item.put("quantity", 2);
        item.put("unitPrice", 250.00);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 1);
        payload.put("shippingAddress", "Av. Lima 123");
        payload.put("shippingCost", 15.00);
        payload.put("notes", "");
        payload.put("items", List.of(item));

        mockMvc.perform(post("/api/orders/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.cotizacionCode").value("COT-2026-00005"));
    }

    @Test
    @DisplayName("POST /api/orders/cotizacion sin userId → 400")
    void createCotizacion_missingUserId_returns400() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("shippingAddress", "Av. Lima 123");
        payload.put("items", List.of());

        mockMvc.perform(post("/api/orders/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/orders/cotizacion usuario no encontrado → 400")
    void createCotizacion_userNotFound_returns400() throws Exception {
        when(userService.getUserById(99L)).thenReturn(null);

        Map<String, Object> item = new HashMap<>();
        item.put("productId", 1);
        item.put("quantity", 1);
        item.put("unitPrice", 100.00);

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", 99);
        payload.put("shippingAddress", "Av. Lima 123");
        payload.put("items", List.of(item));

        mockMvc.perform(post("/api/orders/cotizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }
}
