package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de Caja Negra — ProductController
 * Solo se verifica el contrato HTTP (entradas/salidas): status codes y
 * estructura del JSON. No se conoce ni importa la lógica interna.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController — Tests de Caja Negra")
class ProductControllerBlackBoxTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Product sampleProduct;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController).build();

        sampleProduct = new Product();
        sampleProduct.setId(1L);
        sampleProduct.setName("Tornillo Hexagonal M8");
        sampleProduct.setDescription("Tornillo de acero inoxidable M8 x 25mm");
        sampleProduct.setPrice(new BigDecimal("4.50"));
        sampleProduct.setStock(100);
        sampleProduct.setMinStock(10);
        sampleProduct.setSku(1001L);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/products
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/products → 200 con array de productos")
    void getAllProducts_returns200WithArray() throws Exception {
        when(productService.getAllProducts()).thenReturn(Arrays.asList(sampleProduct));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Tornillo Hexagonal M8"));
    }

    // ─────────────────────────────────────────────────────
    // GET /api/products/featured
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/products/featured → 200 con array")
    void getFeaturedProducts_returns200() throws Exception {
        when(productService.getFeaturedProducts()).thenReturn(Collections.singletonList(sampleProduct));

        mockMvc.perform(get("/api/products/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ─────────────────────────────────────────────────────
    // GET /api/products/{id}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/products/{id} existente → 200 con el producto")
    void getProductById_found_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(sampleProduct);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tornillo Hexagonal M8"));
    }

    @Test
    @DisplayName("GET /api/products/{id} inexistente → 404")
    void getProductById_notFound_returns404() throws Exception {
        when(productService.getProductById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────
    // GET /api/products/category/{categoryId}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/products/category/{id} → 200 con array")
    void getProductsByCategory_returns200() throws Exception {
        when(productService.getProductsByCategory(3L)).thenReturn(Collections.singletonList(sampleProduct));

        mockMvc.perform(get("/api/products/category/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tornillo Hexagonal M8"));
    }

    // ─────────────────────────────────────────────────────
    // POST /api/products
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/products → 200 con el producto creado")
    void createProduct_returns200WithCreatedProduct() throws Exception {
        when(productService.createProduct(any())).thenReturn(sampleProduct);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProduct)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Tornillo Hexagonal M8"));
    }

    // ─────────────────────────────────────────────────────
    // PUT /api/products/{id}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/products/{id} existente → 200 con datos actualizados")
    void updateProduct_found_returns200() throws Exception {
        Product updated = new Product();
        updated.setId(1L);
        updated.setName("Tornillo Actualizado");
        updated.setDescription("Tornillo actualizado de acero M8");
        updated.setPrice(new BigDecimal("5.99"));
        updated.setStock(80);
        when(productService.updateProduct(eq(1L), any())).thenReturn(updated);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tornillo Actualizado"))
                .andExpect(jsonPath("$.price").value(5.99));
    }

    @Test
    @DisplayName("PUT /api/products/{id} inexistente → 404")
    void updateProduct_notFound_returns404() throws Exception {
        when(productService.updateProduct(eq(99L), any())).thenReturn(null);

        mockMvc.perform(put("/api/products/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProduct)))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────
    // DELETE /api/products/{id}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/products/{id} → 204 sin contenido")
    void deleteProduct_returns204() throws Exception {
        doNothing().when(productService).deleteProduct(1L);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());
    }
}
