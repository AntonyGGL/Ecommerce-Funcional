package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_ShouldCalculateTaxAndTotalCorrectly() {
        // Arrange
        User user = new User();
        user.setId(1L);
        
        Product product = new Product();
        product.setId(1L);
        product.setName("Laptop");
        product.setPrice(new BigDecimal("1000.00"));
        product.setStock(10);
        
        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(1);
        detail.setUnitPrice(product.getPrice());
        
        List<OrderDetail> items = new ArrayList<>();
        items.add(detail);
        
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Act
        Order order = orderService.createOrder(user, items, "Main St 123");
        
        // Assert
        // Subtotal = 1000
        // Tax = 1000 * 0.18 = 180
        // Total = 1180
        assertEquals(new BigDecimal("1000.00"), order.getSubtotal());
        assertEquals(new BigDecimal("180.0000"), order.getTax()); // 0.18 rate
        assertEquals(new BigDecimal("1180.0000"), order.getTotal());
    }
}
