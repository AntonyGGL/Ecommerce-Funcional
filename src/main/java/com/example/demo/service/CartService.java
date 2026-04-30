package com.example.demo.service;

import com.example.demo.model.CartItem;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.CartItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {
    @Autowired
    private CartItemRepository cartItemRepository;

    public List<CartItem> getCartItems(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    @Transactional
    public CartItem addToCart(User user, Product product, Integer quantity) {
        // Validate product has stock
        if (product.getStock() == 0) {
            throw new RuntimeException("Producto no disponible: " + product.getName() + " - Sin stock");
        }
        
        Optional<CartItem> existingItem = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());
        
        int totalQuantity = quantity;
        if (existingItem.isPresent()) {
            totalQuantity += existingItem.get().getQuantity();
        }

        if (product.getStock() < totalQuantity) {
            throw new RuntimeException("Stock insuficiente para: " + product.getName() + ". Disponibles: " + product.getStock());
        }

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(totalQuantity);
            return cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem(user, product, quantity);
            return cartItemRepository.save(newItem);
        }
    }

    @Transactional
    public CartItem updateQuantity(Long itemId, Integer quantity) {
        CartItem item = cartItemRepository.findById(itemId).orElse(null);
        if (item != null) {
            Product product = item.getProduct();
            
            // Validate product has stock
            if (product.getStock() == 0) {
                throw new RuntimeException("Producto no disponible: " + product.getName() + " - Sin stock");
            }
            
            // Validate quantity doesn't exceed available stock
            if (quantity > product.getStock()) {
                throw new RuntimeException("Stock insuficiente para: " + product.getName() + ". Disponibles: " + product.getStock());
            }
            
            item.setQuantity(quantity);
            return cartItemRepository.save(item);
        }
        return null;
    }

    @Transactional
    public void removeFromCart(Long itemId) {
        cartItemRepository.deleteById(itemId);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }
}
