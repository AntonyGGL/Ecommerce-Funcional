package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.model.OrderDetail;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests de Caja Blanca â€” OrderService
 * Cubre todas las ramas del cÃ³digo interno.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService â€” Tests de Caja Blanca")
class OrderServiceWhiteBoxTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // createOrder
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("createOrder: calcula subtotal, tax (18%) y total correctamente")
    void createOrder_calculatesSubtotalTaxAndTotal() {
        User user = buildUser(1L);
        OrderDetail detail = buildDetail(new BigDecimal("1000.00"), 2); // subtotal = 2000
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.createOrder(user, List.of(detail), "Av. Lima 123");

        assertEquals(new BigDecimal("2000.00"), order.getSubtotal());
        assertEquals(new BigDecimal("360.0000"), order.getTax());
        assertEquals(new BigDecimal("2360.0000"), order.getTotal());
    }

    @Test
    @DisplayName("createOrder: item con unitPrice null usa cero en el cÃ¡lculo")
    void createOrder_nullUnitPrice_treatedAsZero() {
        User user = buildUser(1L);
        OrderDetail detail = new OrderDetail();
        detail.setUnitPrice(null);
        detail.setQuantity(3);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.createOrder(user, List.of(detail), "Calle 1");

        assertEquals(0, order.getSubtotal().compareTo(BigDecimal.ZERO));
        assertEquals(0, order.getTax().compareTo(BigDecimal.ZERO));
        assertEquals(0, order.getTotal().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("createOrder: item con quantity null usa cero en el cÃ¡lculo")
    void createOrder_nullQuantity_treatedAsZero() {
        User user = buildUser(1L);
        OrderDetail detail = new OrderDetail();
        detail.setUnitPrice(new BigDecimal("500.00"));
        detail.setQuantity(null);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.createOrder(user, List.of(detail), "Calle 2");

        assertEquals(0, order.getSubtotal().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("createOrder: mÃºltiples items acumulan subtotal correctamente")
    void createOrder_multipleItems_accumulatesSubtotal() {
        User user = buildUser(1L);
        OrderDetail d1 = buildDetail(new BigDecimal("100.00"), 2);  // 200
        OrderDetail d2 = buildDetail(new BigDecimal("50.00"),  3);  // 150
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.createOrder(user, new ArrayList<>(List.of(d1, d2)), "Jr. Puno 99");

        assertEquals(new BigDecimal("350.00"), order.getSubtotal());
    }

    @Test
    @DisplayName("createOrder: estado inicial es PENDING para orden y pago")
    void createOrder_initialStatusIsPending() {
        User user = buildUser(1L);
        OrderDetail detail = buildDetail(new BigDecimal("200.00"), 1);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.createOrder(user, List.of(detail), "Av. Central");

        assertEquals(Order.OrderStatus.PENDING, order.getStatus());
        assertEquals(Order.PaymentStatus.PENDING, order.getPaymentStatus());
    }

    @Test
    @DisplayName("createOrder: asocia cada item a la orden creada")
    void createOrder_setsOrderReferenceOnEachDetail() {
        User user = buildUser(1L);
        OrderDetail d1 = buildDetail(new BigDecimal("10.00"), 1);
        OrderDetail d2 = buildDetail(new BigDecimal("20.00"), 1);
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order order = orderService.createOrder(user, new ArrayList<>(List.of(d1, d2)), "Jr. Ancash");

        order.getOrderDetails().forEach(d -> assertSame(order, d.getOrder()));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // updateShippingStatus
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("updateShippingStatus: DELIVERED tambiÃ©n marca la orden como COMPLETED")
    void updateShippingStatus_delivered_setsOrderCompleted() {
        Order order = new Order();
        order.setStatus(Order.OrderStatus.PROCESSING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order updated = orderService.updateShippingStatus(1L, Order.ShippingStatus.DELIVERED);

        assertEquals(Order.ShippingStatus.DELIVERED, updated.getShippingStatus());
        assertEquals(Order.OrderStatus.COMPLETED, updated.getStatus());
    }

    @Test
    @DisplayName("updateShippingStatus: SHIPPED no cambia el estado de la orden")
    void updateShippingStatus_shipped_doesNotChangeOrderStatus() {
        Order order = new Order();
        order.setStatus(Order.OrderStatus.PROCESSING);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order updated = orderService.updateShippingStatus(2L, Order.ShippingStatus.SHIPPED);

        assertEquals(Order.ShippingStatus.SHIPPED, updated.getShippingStatus());
        assertEquals(Order.OrderStatus.PROCESSING, updated.getStatus());
    }

    @Test
    @DisplayName("updateShippingStatus: orden inexistente retorna null")
    void updateShippingStatus_notFound_returnsNull() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        Order result = orderService.updateShippingStatus(99L, Order.ShippingStatus.DELIVERED);

        assertNull(result);
        verify(orderRepository, never()).save(any());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // updateShippingAddress
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("updateShippingAddress: actualiza la direcciÃ³n correctamente")
    void updateShippingAddress_updatesAddress() {
        Order order = new Order();
        order.setShippingAddress("Vieja direcciÃ³n");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order updated = orderService.updateShippingAddress(1L, "Nueva direcciÃ³n 456");

        assertEquals("Nueva direcciÃ³n 456", updated.getShippingAddress());
    }

    @Test
    @DisplayName("updateShippingAddress: orden inexistente retorna null")
    void updateShippingAddress_notFound_returnsNull() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(orderService.updateShippingAddress(99L, "Cualquier cosa"));
        verify(orderRepository, never()).save(any());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // decreaseStock
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    @DisplayName("decreaseStock: reduce el stock de cada producto involucrado")
    void decreaseStock_reducesProductStock() {
        Product product = new Product();
        product.setId(1L);
        product.setStock(10);

        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(3);

        Order order = new Order();
        order.setOrderDetails(List.of(detail));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        orderService.decreaseStock(order);

        assertEquals(7, product.getStock());
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("decreaseStock: producto no encontrado no lanza excepciÃ³n")
    void decreaseStock_productNotFound_noException() {
        Product ghost = new Product();
        ghost.setId(999L);

        OrderDetail detail = new OrderDetail();
        detail.setProduct(ghost);
        detail.setQuantity(1);

        Order order = new Order();
        order.setOrderDetails(List.of(detail));

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> orderService.decreaseStock(order));
        verify(productRepository, never()).save(any());
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Helpers
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    // ─────────────────────────────────────────────────────
    // updateOrderStatus
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateOrderStatus: actualiza el estado cuando la orden existe")
    void updateOrderStatus_found_updatesStatus() {
        Order order = new Order();
        order.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order updated = orderService.updateOrderStatus(1L, Order.OrderStatus.PROCESSING);

        assertEquals(Order.OrderStatus.PROCESSING, updated.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("updateOrderStatus: retorna null cuando la orden no existe")
    void updateOrderStatus_notFound_returnsNull() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        Order result = orderService.updateOrderStatus(99L, Order.OrderStatus.COMPLETED);

        assertNull(result);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateOrderStatus: puede pasar a CANCELLED desde PENDING")
    void updateOrderStatus_pendingToCancelled_updatesStatus() {
        Order order = new Order();
        order.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order updated = orderService.updateOrderStatus(2L, Order.OrderStatus.CANCELLED);

        assertEquals(Order.OrderStatus.CANCELLED, updated.getStatus());
    }

    // ─────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────
    // confirmarCotizacion
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmarCotizacion: cambia estado a PENDING y descuenta stock al confirmar")
    void confirmarCotizacion_valid_changesPendingAndDecreasesStock() {
        Product product = new Product();
        product.setId(1L);
        product.setStock(10);

        OrderDetail detail = new OrderDetail();
        detail.setProduct(product);
        detail.setQuantity(3);

        Order order = new Order();
        order.setStatus(Order.OrderStatus.COTIZACION);
        order.setOrderDetails(List.of(detail));

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.confirmarCotizacion(1L);

        assertEquals(Order.OrderStatus.PENDING, result.getStatus());
        assertEquals(7, product.getStock(), "El stock debe reducirse en la cantidad pedida");
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("confirmarCotizacion: retorna null cuando la orden no existe")
    void confirmarCotizacion_notFound_returnsNull() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(orderService.confirmarCotizacion(99L));
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("confirmarCotizacion: retorna null cuando la orden no está en estado COTIZACION")
    void confirmarCotizacion_wrongStatus_returnsNull() {
        Order order = new Order();
        order.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        assertNull(orderService.confirmarCotizacion(2L));
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────
    // ignorarCotizacion
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("ignorarCotizacion: cambia estado a IGNORED")
    void ignorarCotizacion_valid_changesStatusToIgnored() {
        Order order = new Order();
        order.setStatus(Order.OrderStatus.COTIZACION);
        order.setOrderDetails(new ArrayList<>());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.ignorarCotizacion(1L);

        assertEquals(Order.OrderStatus.IGNORED, result.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("ignorarCotizacion: retorna null cuando la orden no existe")
    void ignorarCotizacion_notFound_returnsNull() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertNull(orderService.ignorarCotizacion(99L));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("ignorarCotizacion: retorna null cuando la orden no está en estado COTIZACION")
    void ignorarCotizacion_wrongStatus_returnsNull() {
        Order order = new Order();
        order.setStatus(Order.OrderStatus.PENDING);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));

        assertNull(orderService.ignorarCotizacion(3L));
        verify(orderRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────
    // getTotalRevenue / getTotalOrders (excluyen COTIZACION e IGNORED)
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getTotalRevenue: delega en sumConfirmedRevenue y excluye cotizaciones")
    void getTotalRevenue_delegatesToSumConfirmedRevenue() {
        when(orderRepository.sumConfirmedRevenue()).thenReturn(new BigDecimal("18530.00"));

        BigDecimal result = orderService.getTotalRevenue();

        assertEquals(new BigDecimal("18530.00"), result);
        verify(orderRepository).sumConfirmedRevenue();
    }

    @Test
    @DisplayName("getTotalRevenue: retorna ZERO cuando el repositorio devuelve null")
    void getTotalRevenue_nullFromRepo_returnsZero() {
        when(orderRepository.sumConfirmedRevenue()).thenReturn(null);

        assertEquals(BigDecimal.ZERO, orderService.getTotalRevenue());
    }

    @Test
    @DisplayName("getTotalOrders: delega en countConfirmedOrders y excluye cotizaciones")
    void getTotalOrders_delegatesToCountConfirmedOrders() {
        when(orderRepository.countConfirmedOrders()).thenReturn(12L);

        assertEquals(12L, orderService.getTotalOrders());
        verify(orderRepository).countConfirmedOrders();
    }

    // ─────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────

    private User buildUser(Long id) {
        User u = new User();
        u.setId(id);
        return u;
    }

    private OrderDetail buildDetail(BigDecimal price, int qty) {
        OrderDetail d = new OrderDetail();
        d.setUnitPrice(price);
        d.setQuantity(qty);
        return d;
    }
}
