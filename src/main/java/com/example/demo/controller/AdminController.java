package com.example.demo.controller;

import com.example.demo.model.Category;
import com.example.demo.model.Order;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.OrderDetailRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ReclamacionRepository;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ReclamacionRepository reclamacionRepository;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSales", orderService.getTotalRevenue());
        stats.put("totalOrders", orderService.getTotalOrders());
        stats.put("totalClients", userService.getAllUsers().size());
        stats.put("totalProducts", productService.getAllProducts().size());
        stats.put("lowStockCount", productService.getLowStockProducts().size());
        
        // Agregar órdenes recientes al dashboard
        Page<Order> recentOrders = orderService.getOrdersPagedAndFiltered(null, null, null, PageRequest.of(0, 5, Sort.by("id").descending()));
        stats.put("recentOrders", recentOrders.getContent());

        // Agregar estadísticas para el gráfico (últimos 7 días)
        stats.put("chartData", orderService.getDailySalesStats(7));
        
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockAlerts() {
        return ResponseEntity.ok(productService.getLowStockProducts());
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String role) {
        
        Page<User> userPage;
        
        if (role != null && !role.isEmpty()) {
            try {
                User.UserRole userRole = User.UserRole.valueOf(role.toUpperCase());
                userPage = userRepository.findByRole(userRole, PageRequest.of(page, size, Sort.by("id").descending()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        } else {
            userPage = userRepository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
        }
        
        List<Map<String, Object>> response = userPage.getContent().stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("email", user.getEmail());
            map.put("firstName", user.getFirstName());
            map.put("lastName", user.getLastName());
            map.put("company", user.getCompany());
            map.put("role", user.getRole());
            map.put("active", user.getActive());
            map.put("createdAt", user.getCreatedAt());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("users", response);
        result.put("currentPage", userPage.getNumber());
        result.put("totalItems", userPage.getTotalElements());
        result.put("totalPages", userPage.getTotalPages());
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/users")
    public ResponseEntity<User> createUser(@jakarta.validation.Valid @RequestBody User user) {
        if (user.getRole() == null) {
            user.setRole(User.UserRole.CUSTOMER);
        }
        return ResponseEntity.ok(userService.createUser(user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categories")
    public ResponseEntity<List<Category>> getAllCategories() {
        return ResponseEntity.ok(categoryRepository.findAll());
    }

    @GetMapping("/orders")
    public ResponseEntity<Map<String, Object>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        
        String searchTerm = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        
        Page<Order> orderPage = orderService.getOrdersPagedAndFiltered(
                searchTerm, 
                startDateTime, 
                endDateTime, 
                PageRequest.of(page, size, Sort.by("id").descending())
        );

        List<Map<String, Object>> ordersList = orderPage.getContent().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("total", order.getTotal());
            map.put("status", order.getStatus() != null ? order.getStatus().name() : "PENDING");
            map.put("paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING");
            map.put("shippingStatus", order.getShippingStatus() != null ? order.getShippingStatus().name() : "PENDING");
            map.put("createdAt", order.getCreatedAt());
            map.put("shippingAddress", order.getShippingAddress());
            map.put("notes", order.getNotes());

            List<Map<String, Object>> details = order.getOrderDetails().stream().map(detail -> {
                Map<String, Object> d = new HashMap<>();
                d.put("productName", detail.getProduct() != null ? detail.getProduct().getName() : "Producto");
                d.put("quantity", detail.getQuantity());
                d.put("unitPrice", detail.getUnitPrice());
                d.put("subtotal", detail.getSubtotal());
                return d;
            }).collect(Collectors.toList());
            map.put("orderDetails", details);
            
            String method = "ENVÍO A DOMICILIO";
            if (order.getShippingAddress() != null && 
                order.getShippingAddress().toLowerCase().contains("recojo")) {
                method = "RECOJO EN TIENDA";
            }
            map.put("shippingMethod", method);

            Map<String, Object> userMap = new HashMap<>();
            if (order.getUser() != null) {
                userMap.put("name", order.getUser().getFirstName() + " " + order.getUser().getLastName());
                userMap.put("email", order.getUser().getEmail());
                userMap.put("company", order.getUser().getCompany());
                userMap.put("firstName", order.getUser().getFirstName());
                userMap.put("lastName", order.getUser().getLastName());
            }
            map.put("customer", userMap);
            
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("orders", ordersList);
        response.put("currentPage", orderPage.getNumber());
        response.put("totalItems", orderPage.getTotalElements());
        response.put("totalPages", orderPage.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/orders/{id}/shipping-status")
    public ResponseEntity<Order> updateShippingStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String statusStr = payload.get("status");
        try {
            Order.ShippingStatus status = Order.ShippingStatus.valueOf(statusStr.toUpperCase());
            Order updatedOrder = orderService.updateShippingStatus(id, status);
            return updatedOrder != null ? ResponseEntity.ok(updatedOrder) : ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/cotizaciones")
    public ResponseEntity<List<Map<String, Object>>> getCotizaciones() {
        List<Order> cotizaciones = orderService.getCotizaciones();
        List<Map<String, Object>> result = cotizaciones.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("cotizacionCode", order.getCotizacionCode());
            map.put("status", order.getStatus().name());
            map.put("total", order.getTotal());
            map.put("shippingAddress", order.getShippingAddress());
            map.put("notes", order.getNotes());
            map.put("createdAt", order.getCreatedAt());
            if (order.getUser() != null) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", order.getUser().getFirstName() + " " + order.getUser().getLastName());
                customer.put("email", order.getUser().getEmail());
                map.put("customer", customer);
            }
            List<Map<String, Object>> details = order.getOrderDetails().stream().map(detail -> {
                Map<String, Object> d = new HashMap<>();
                d.put("productName", detail.getProduct() != null ? detail.getProduct().getName() : "Producto");
                d.put("quantity", detail.getQuantity());
                d.put("unitPrice", detail.getUnitPrice());
                d.put("subtotal", detail.getSubtotal());
                return d;
            }).collect(Collectors.toList());
            map.put("items", details);
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PutMapping("/cotizaciones/{id}/confirmar")
    public ResponseEntity<Order> confirmarCotizacion(@PathVariable Long id) {
        Order order = orderService.confirmarCotizacion(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @PutMapping("/cotizaciones/{id}/ignorar")
    public ResponseEntity<Order> ignorarCotizacion(@PathVariable Long id) {
        Order order = orderService.ignorarCotizacion(id);
        return order != null ? ResponseEntity.ok(order) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/cleanup/orders")
    public ResponseEntity<Map<String, Object>> cleanupOrders() {        try {
            // Delete in FK order
            cartItemRepository.deleteAll();
            orderDetailRepository.deleteAll();
            orderRepository.deleteAll();
            reclamacionRepository.deleteAll();
            productRepository.deleteAll();
            // NOT deleting categories — they are base configuration data
            // Keep ADMIN users, delete only CUSTOMER accounts
            userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && !u.getRole().name().equals("ADMIN"))
                .forEach(userRepository::delete);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Todos los datos de prueba han sido eliminados correctamente.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error during cleanup: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
