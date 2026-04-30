package com.example.demo.service;

import com.example.demo.model.CartItem;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.CartItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de Caja Blanca — CartService
 * Cubre todas las ramas: stock=0, stock insuficiente,
 * item nuevo, item existente, updateQuantity.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartService — Tests de Caja Blanca")
class CartServiceWhiteBoxTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartService cartService;

    // ─────────────────────────────────────────────────────
    // addToCart — rama: stock == 0
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("addToCart: lanza excepción cuando el producto no tiene stock")
    void addToCart_stockZero_throwsProductNotAvailable() {
        Product product = buildProduct(1L, "Taladro", 0);
        User user = buildUser(1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.addToCart(user, product, 1));

        assertTrue(ex.getMessage().contains("Sin stock"));
        verify(cartItemRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────
    // addToCart — rama: item nuevo, stock suficiente
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("addToCart: crea nuevo CartItem cuando el producto no estaba en el carrito")
    void addToCart_newItem_createsCartItem() {
        Product product = buildProduct(1L, "Sierra", 10);
        User user = buildUser(1L);

        when(cartItemRepository.findByUserIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CartItem result = cartService.addToCart(user, product, 3);

        assertNotNull(result);
        verify(cartItemRepository).save(any(CartItem.class));
    }

    // ─────────────────────────────────────────────────────
    // addToCart — rama: item existente, stock suficiente
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("addToCart: actualiza cantidad cuando el item ya existe en el carrito")
    void addToCart_existingItem_updatesQuantity() {
        Product product = buildProduct(1L, "Amoladora", 10);
        User user = buildUser(1L);

        CartItem existing = new CartItem();
        existing.setQuantity(2);
        existing.setProduct(product);

        when(cartItemRepository.findByUserIdAndProductId(1L, 1L)).thenReturn(Optional.of(existing));
        when(cartItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CartItem result = cartService.addToCart(user, product, 3); // 2 + 3 = 5 ≤ 10

        assertEquals(5, result.getQuantity());
        verify(cartItemRepository).save(existing);
    }

    // ─────────────────────────────────────────────────────
    // addToCart — rama: stock insuficiente (item nuevo)
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("addToCart: lanza excepción cuando la cantidad pedida supera el stock disponible")
    void addToCart_newItem_insufficientStock_throws() {
        Product product = buildProduct(1L, "Compresor", 5);
        User user = buildUser(1L);

        when(cartItemRepository.findByUserIdAndProductId(1L, 1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.addToCart(user, product, 10));

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(cartItemRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────
    // addToCart — rama: stock insuficiente (item existente)
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("addToCart: lanza excepción cuando la suma con el existente supera el stock")
    void addToCart_existingItem_insufficientStock_throws() {
        Product product = buildProduct(1L, "Fresadora", 5);
        User user = buildUser(1L);

        CartItem existing = new CartItem();
        existing.setQuantity(4); // ya tiene 4, pide 3 más → 7 > 5

        when(cartItemRepository.findByUserIdAndProductId(1L, 1L)).thenReturn(Optional.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.addToCart(user, product, 3));

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
        verify(cartItemRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────
    // updateQuantity
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateQuantity: retorna null cuando el item no existe")
    void updateQuantity_itemNotFound_returnsNull() {
        when(cartItemRepository.findById(99L)).thenReturn(Optional.empty());

        CartItem result = cartService.updateQuantity(99L, 2);

        assertNull(result);
        verify(cartItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateQuantity: lanza excepción cuando el producto tiene stock 0")
    void updateQuantity_stockZero_throws() {
        Product product = buildProduct(1L, "Torno", 0);
        CartItem item = new CartItem();
        item.setProduct(product);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.updateQuantity(1L, 1));

        assertTrue(ex.getMessage().contains("Sin stock"));
    }

    @Test
    @DisplayName("updateQuantity: lanza excepción cuando la cantidad supera el stock")
    void updateQuantity_quantityExceedsStock_throws() {
        Product product = buildProduct(1L, "Esmeril", 3);
        CartItem item = new CartItem();
        item.setProduct(product);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> cartService.updateQuantity(1L, 5));

        assertTrue(ex.getMessage().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("updateQuantity: actualiza la cantidad cuando es válida")
    void updateQuantity_validQuantity_updatesItem() {
        Product product = buildProduct(1L, "Pulidora", 10);
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1);

        when(cartItemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CartItem result = cartService.updateQuantity(1L, 4);

        assertEquals(4, result.getQuantity());
        verify(cartItemRepository).save(item);
    }

    // ─────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────

    private Product buildProduct(Long id, String name, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setStock(stock);
        return p;
    }

    private User buildUser(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }
}
