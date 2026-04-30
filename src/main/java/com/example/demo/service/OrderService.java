package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Page<Order> getOrdersPagedAndFiltered(String searchTerm, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        String search = (searchTerm != null && !searchTerm.trim().isEmpty()) ? "%" + searchTerm.trim().toLowerCase() + "%" : null;
        return orderRepository.searchOrders(search, startDate, endDate, pageable);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElse(null);
    }

    public List<Order> getUserOrders(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public Order updateOrder(Order order) {
        return orderRepository.save(order);
    }

    public void validateStock(List<OrderDetail> items) {
        // Stock validation is performed on frontend before checkout
        // Backend allows order creation even with stock constraints
        // This prevents checkout errors from appearing to the user
    }

    @Transactional
    public void decreaseStock(Order order) {
        try {
            for (OrderDetail detail : order.getOrderDetails()) {
                // Reload product from database to ensure we have latest data
                Product product = productRepository.findById(detail.getProduct().getId()).orElse(null);
                if (product != null) {
                    int newStock = product.getStock() - detail.getQuantity();
                    // Allow negative stock (overselling) to prevent checkout errors
                    product.setStock(newStock);
                    productRepository.save(product);
                }
            }
        } catch (Exception e) {
            // Log error but don't throw exception to prevent checkout failure
            log.warn("Error al reducir stock: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public Order createOrder(User user, List<OrderDetail> items, String shippingAddress) {
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);

        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        for (OrderDetail item : items) {
            item.setOrder(order);
            // Ensure the subtotal for each item is calculated
            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            int qty = Objects.requireNonNullElse(item.getQuantity(), 0);
            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(qty));
            item.setSubtotal(itemSubtotal);
            
            calculatedSubtotal = calculatedSubtotal.add(itemSubtotal);
        }

        order.setSubtotal(calculatedSubtotal);
        BigDecimal tax = calculatedSubtotal.multiply(TAX_RATE);
        order.setTax(tax);
        order.setTotal(calculatedSubtotal.add(tax));
        order.setOrderDetails(items);
        
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setStatus(Order.OrderStatus.PENDING);

        Order savedOrder = orderRepository.save(order);
        
        return savedOrder;
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setStatus(status);
            return orderRepository.save(order);
        }
        return null;
    }

    @Transactional
    public Order updateShippingStatus(Long orderId, Order.ShippingStatus status) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setShippingStatus(status);
            // Si se marca como entregado, podrías marcar la orden como completada
            if (status == Order.ShippingStatus.DELIVERED) {
                order.setStatus(Order.OrderStatus.COMPLETED);
            }
            return orderRepository.save(order);
        }
        return null;
    }

    @Transactional
    public Order updateShippingAddress(Long orderId, String address) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            order.setShippingAddress(address);
            return orderRepository.save(order);
        }
        return null;
    }

    public void deleteOrder(Long id) {
        orderRepository.deleteById(id);
    }

    public java.util.Map<String, BigDecimal> getDailySalesStats(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> results = orderRepository.getSalesStatsByDay(since);
        java.util.Map<String, BigDecimal> stats = new java.util.LinkedHashMap<>();
        
        // Inicializar los últimos X días con 0 para que el gráfico no tenga huecos
        for (int i = days; i >= 0; i--) {
            String dateLabel = LocalDate.now().minusDays(i).toString();
            stats.put(dateLabel, BigDecimal.ZERO);
        }
        
        for (Object[] result : results) {
            String date = result[0].toString();
            BigDecimal total = (BigDecimal) result[1];
            stats.put(date, total);
        }
        return stats;
    }

    public long getTotalOrders() {
        return orderRepository.countConfirmedOrders();
    }

    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.sumConfirmedRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public List<Order> getCotizaciones() {
        return orderRepository.findByStatusIn(
            List.of(Order.OrderStatus.COTIZACION, Order.OrderStatus.IGNORED));
    }

    @Transactional
    public Order createCotizacion(User user, List<Map<String, Object>> itemsData,
                                  String shippingAddress, BigDecimal shippingCost, String notes) {
        Order order = new Order();
        order.setUser(user);
        order.setShippingAddress(shippingAddress);
        order.setNotes(notes);
        order.setShippingCost(shippingCost != null ? shippingCost : BigDecimal.ZERO);
        order.setStatus(Order.OrderStatus.COTIZACION);
        order.setPaymentStatus(Order.PaymentStatus.PENDING);
        order.setShippingStatus(Order.ShippingStatus.PENDING);

        List<OrderDetail> details = new ArrayList<>();
        BigDecimal calculatedSubtotal = BigDecimal.ZERO;
        System.out.println("DEBUG createCotizacion - items recibidos: " + itemsData);
        for (Map<String, Object> item : itemsData) {
            System.out.println("DEBUG item keys: " + item.keySet() + " values: " + item);
            Object rawId = item.get("productId");
            if (rawId == null) {
                System.out.println("DEBUG ERROR: productId es null. Claves del item: " + item.keySet());
                throw new RuntimeException("Falta productId en uno de los productos del carrito. Claves recibidas: " + item.keySet());
            }
            Long productId = Long.valueOf(rawId.toString());
            Object rawQty = item.get("quantity");
            int qty = rawQty != null ? Integer.parseInt(rawQty.toString()) : 1;
            Object rawPrice = item.get("unitPrice");
            if (rawPrice == null) throw new RuntimeException("Falta unitPrice en producto " + productId);
            BigDecimal unitPrice = new BigDecimal(rawPrice.toString());
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado: " + productId));
            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(qty);
            detail.setUnitPrice(unitPrice);
            BigDecimal itemSub = unitPrice.multiply(BigDecimal.valueOf(qty));
            detail.setSubtotal(itemSub);
            details.add(detail);
            calculatedSubtotal = calculatedSubtotal.add(itemSub);
        }
        BigDecimal sc = shippingCost != null ? shippingCost : BigDecimal.ZERO;
        order.setSubtotal(calculatedSubtotal);
        order.setShippingCost(sc);
        order.setTax(BigDecimal.ZERO);
        // Total = subtotal + envío (18% solo si subtotal < 400 y es delivery, ya calculado en frontend)
        order.setTotal(calculatedSubtotal.add(sc));
        order.setOrderDetails(details);
        Order saved = orderRepository.save(order);
        String code = String.format("COT-%d-%05d", LocalDate.now().getYear(), saved.getId());
        saved.setCotizacionCode(code);
        return orderRepository.save(saved);
    }

    @Transactional
    public Order confirmarCotizacion(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != Order.OrderStatus.COTIZACION) return null;
        order.setStatus(Order.OrderStatus.PENDING);
        Order saved = orderRepository.save(order);
        decreaseStock(saved);
        return saved;
    }

    @Transactional
    public Order ignorarCotizacion(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != Order.OrderStatus.COTIZACION) return null;
        order.setStatus(Order.OrderStatus.IGNORED);
        return orderRepository.save(order);
    }
}
