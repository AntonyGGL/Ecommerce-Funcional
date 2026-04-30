package com.example.demo.service;

import com.example.demo.model.CartItem;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.CartItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private CartService cartService;

    @Test
    void addToCart_ShouldThrowException_WhenStockIsInsufficient() {
        // Arrange
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(1L);
        product.setStock(5);
        product.setName("Test Product");
        
        when(cartItemRepository.findByUserIdAndProductId(1L, 1L)).thenReturn(Optional.empty());
        
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            cartService.addToCart(user, product, 10);
        });
        
        assertEquals("Stock insuficiente para: Test Product. Disponibles: 5", exception.getMessage());
    }

    @Test
    void addToCart_ShouldThrowException_WhenUpdateLeadsToInsufficientStock() {
        // Arrange
        User user = new User();
        user.setId(1L);
        Product product = new Product();
        product.setId(1L);
        product.setStock(15);
        product.setName("Test Product");

        CartItem existingItem = new CartItem(user, product, 10);
        
        when(cartItemRepository.findByUserIdAndProductId(1L, 1L)).thenReturn(Optional.of(existingItem));
        
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            cartService.addToCart(user, product, 10); // 10 + 10 = 20 > 15
        });
        
        assertEquals("Stock insuficiente para: Test Product. Disponibles: 15", exception.getMessage());
    }
}
