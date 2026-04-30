package com.example.demo.service;

import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de Caja Blanca — ProductService
 * Cubre ramas de createProduct (SKU/minStock defaults)
 * y updateProduct (found / not found).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService — Tests de Caja Blanca")
class ProductServiceWhiteBoxTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    // ─────────────────────────────────────────────────────
    // createProduct — rama: SKU null → auto-generar
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createProduct: asigna SKU automático cuando es null")
    void createProduct_nullSku_assignsAutoSku() {
        Product product = new Product();
        product.setSku(null);
        product.setMinStock(3);

        when(productRepository.findNextSku()).thenReturn(10042L);
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Product saved = productService.createProduct(product);

        assertEquals(10042L, saved.getSku());
        verify(productRepository).findNextSku();
    }

    // ─────────────────────────────────────────────────────
    // createProduct — rama: SKU proporcionado → no tocar
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createProduct: conserva el SKU cuando ya viene definido")
    void createProduct_providedSku_keepsSku() {
        Product product = new Product();
        product.setSku(12001L);
        product.setMinStock(5);

        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Product saved = productService.createProduct(product);

        assertEquals(12001L, saved.getSku());
        verify(productRepository, never()).findNextSku();
    }

    // ─────────────────────────────────────────────────────
    // createProduct — rama: minStock null → default 5
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createProduct: asigna minStock = 5 cuando viene null")
    void createProduct_nullMinStock_setsDefaultFive() {
        Product product = new Product();
        product.setSku(1001L);
        product.setMinStock(null);

        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Product saved = productService.createProduct(product);

        assertEquals(5, saved.getMinStock());
    }

    // ─────────────────────────────────────────────────────
    // createProduct — rama: minStock proporcionado → mantener
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createProduct: conserva minStock cuando viene definido")
    void createProduct_providedMinStock_keepsValue() {
        Product product = new Product();
        product.setSku(1002L);
        product.setMinStock(10);

        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Product saved = productService.createProduct(product);

        assertEquals(10, saved.getMinStock());
    }

    // ─────────────────────────────────────────────────────
    // createProduct — siempre marca active = true
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createProduct: siempre establece active = true")
    void createProduct_alwaysSetsActiveTrue() {
        Product product = new Product();
        product.setSku(1003L);
        product.setMinStock(5);
        product.setActive(false);

        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Product saved = productService.createProduct(product);

        assertTrue(saved.getActive());
    }

    // ─────────────────────────────────────────────────────
    // updateProduct — rama: producto encontrado
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProduct: actualiza todos los campos cuando el producto existe")
    void updateProduct_found_updatesFields() {
        Product existing = new Product();
        existing.setId(1L);
        existing.setName("Viejo nombre");
        existing.setPrice(new BigDecimal("100.00"));
        existing.setStock(5);

        Product updates = new Product();
        updates.setName("Nuevo nombre");
        updates.setPrice(new BigDecimal("200.00"));
        updates.setStock(15);
        updates.setMinStock(3);
        updates.setDescription("Actualizado");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Product result = productService.updateProduct(1L, updates);

        assertNotNull(result);
        assertEquals("Nuevo nombre", result.getName());
        assertEquals(new BigDecimal("200.00"), result.getPrice());
        assertEquals(15, result.getStock());
        assertEquals(3, result.getMinStock());
    }

    // ─────────────────────────────────────────────────────
    // updateProduct — rama: minStock null en update → no se sobreescribe
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProduct: no sobreescribe minStock cuando viene null en el update")
    void updateProduct_nullMinStock_keepsExistingMinStock() {
        Product existing = new Product();
        existing.setId(1L);
        existing.setMinStock(8);

        Product updates = new Product();
        updates.setName("Producto X");
        updates.setMinStock(null);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Product result = productService.updateProduct(1L, updates);

        assertEquals(8, result.getMinStock());
    }

    // ─────────────────────────────────────────────────────
    // updateProduct — rama: producto no encontrado
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProduct: retorna null cuando el producto no existe")
    void updateProduct_notFound_returnsNull() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        Product result = productService.updateProduct(99L, new Product());

        assertNull(result);
        verify(productRepository, never()).save(any());
    }
}
