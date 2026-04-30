package com.example.demo.controller;

import com.example.demo.model.CartItem;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.service.CartService;
import com.example.demo.service.ProductService;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de Caja Negra — CartController
 * Se verifica solo el contrato HTTP: status codes y estructura JSON.
 * Los servicios se mockean; no se conoce la lógica interna del controlador.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartController — Tests de Caja Negra")
class CartControllerBlackBoxTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartController cartController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User testUser;
    private Product testProduct;
    private CartItem testCartItem;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(cartController).build();

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("cliente@test.com");

        testProduct = new Product();
        testProduct.setId(2L);
        testProduct.setName("Tuerca M6");
        testProduct.setPrice(new BigDecimal("1.20"));
        testProduct.setStock(50);

        testCartItem = new CartItem();
        testCartItem.setId(10L);
        testCartItem.setQuantity(3);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/cart/{userId}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/cart/{userId} → 200 con lista de items del carrito")
    void getCart_returns200WithItemList() throws Exception {
        when(cartService.getCartItems(1L)).thenReturn(Collections.singletonList(testCartItem));

        mockMvc.perform(get("/api/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].quantity").value(3));
    }

    // ─────────────────────────────────────────────────────
    // POST /api/cart/{userId}/add
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/cart/{userId}/add usuario y producto válidos → 200 con CartItem")
    void addToCart_validUserAndProduct_returns200() throws Exception {
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(productService.getProductById(2L)).thenReturn(testProduct);
        when(cartService.addToCart(any(), any(), eq(2))).thenReturn(testCartItem);

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", 2);
        payload.put("quantity", 2);

        mockMvc.perform(post("/api/cart/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("POST /api/cart/{userId}/add usuario inexistente → 400")
    void addToCart_userNotFound_returns400() throws Exception {
        when(userService.getUserById(99L)).thenReturn(null);
        when(productService.getProductById(2L)).thenReturn(testProduct);

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", 2);
        payload.put("quantity", 1);

        mockMvc.perform(post("/api/cart/99/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/cart/{userId}/add producto inexistente → 400")
    void addToCart_productNotFound_returns400() throws Exception {
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(productService.getProductById(99L)).thenReturn(null);

        Map<String, Object> payload = new HashMap<>();
        payload.put("productId", 99);
        payload.put("quantity", 1);

        mockMvc.perform(post("/api/cart/1/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────
    // PUT /api/cart/item/{itemId}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/cart/item/{itemId} item existente → 200 con cantidad actualizada")
    void updateQuantity_found_returns200() throws Exception {
        testCartItem.setQuantity(7);
        when(cartService.updateQuantity(10L, 7)).thenReturn(testCartItem);

        Map<String, Integer> payload = new HashMap<>();
        payload.put("quantity", 7);

        mockMvc.perform(put("/api/cart/item/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(7));
    }

    @Test
    @DisplayName("PUT /api/cart/item/{itemId} item inexistente → 404")
    void updateQuantity_notFound_returns404() throws Exception {
        when(cartService.updateQuantity(99L, 5)).thenReturn(null);

        Map<String, Integer> payload = new HashMap<>();
        payload.put("quantity", 5);

        mockMvc.perform(put("/api/cart/item/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────
    // DELETE /api/cart/item/{itemId}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/cart/item/{itemId} → 204 sin contenido")
    void removeFromCart_returns204() throws Exception {
        doNothing().when(cartService).removeFromCart(10L);

        mockMvc.perform(delete("/api/cart/item/10"))
                .andExpect(status().isNoContent());
    }

    // ─────────────────────────────────────────────────────
    // DELETE /api/cart/user/{userId}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/cart/user/{userId} → 204 carrito vaciado")
    void clearCart_returns204() throws Exception {
        doNothing().when(cartService).clearCart(1L);

        mockMvc.perform(delete("/api/cart/user/1"))
                .andExpect(status().isNoContent());
    }
}
