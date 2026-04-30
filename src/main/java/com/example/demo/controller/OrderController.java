package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.User;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id) {
        Order order = orderService.getOrderById(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Order>> getUserOrders(@PathVariable Long userId) {
        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody Map<String, Object> payload) {
        try {
            Long userId = Long.valueOf(payload.get("userId").toString());
            User user = userService.getUserById(userId);
            if (user == null) return ResponseEntity.badRequest().build();

            String shippingAddress = payload.get("shippingAddress").toString();
            @SuppressWarnings("unchecked")
            List<OrderDetail> items = (List<OrderDetail>) payload.get("items");

            Order order = orderService.createOrder(user, items, shippingAddress);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Order> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        Order order = orderService.updateOrderStatus(id, orderStatus);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/confirm-delivery")
    public ResponseEntity<Order> confirmDelivery(@PathVariable Long id) {
        Order order = orderService.updateShippingStatus(id, Order.ShippingStatus.DELIVERED);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/address")
    public ResponseEntity<Order> updateShippingAddress(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String address = payload.get("address");
        Order order = orderService.updateShippingAddress(id, address);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @PostMapping("/cotizacion")
    public ResponseEntity<?> createCotizacion(@RequestBody Map<String, Object> payload) {
        try {
            Object rawUserId = payload.get("userId");
            if (rawUserId == null) return ResponseEntity.badRequest().body(Map.of("error", "Sesión inválida, por favor ingresa nuevamente"));
            Long userId = Long.valueOf(rawUserId.toString());
            User user = userService.getUserById(userId);
            if (user == null) return ResponseEntity.badRequest().body(Map.of("error", "Usuario no encontrado"));
            String shippingAddress = payload.getOrDefault("shippingAddress", "").toString();
            Object rawShipping = payload.get("shippingCost");
            BigDecimal shippingCost = rawShipping != null ? new BigDecimal(rawShipping.toString()) : BigDecimal.ZERO;
            String notes = payload.getOrDefault("notes", "").toString();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) payload.get("items");
            if (items == null || items.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "El carrito está vacío"));
            Order order = orderService.createCotizacion(user, items, shippingAddress, shippingCost, notes);
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
