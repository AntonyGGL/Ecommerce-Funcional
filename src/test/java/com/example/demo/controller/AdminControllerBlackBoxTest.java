package com.example.demo.controller;

import com.example.demo.model.Category;
import com.example.demo.model.Order;
import com.example.demo.model.Product;
import com.example.demo.model.User;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.OrderService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.Pageable;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de Caja Negra — AdminController
 * Verifica el contrato HTTP de todos los endpoints admin:
 * status codes, estructura JSON y campos clave.
 * La lógica interna de servicios se mockea completamente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController — Tests de Caja Negra")
class AdminControllerBlackBoxTest {

    private MockMvc mockMvc;

    @Mock private OrderService   orderService;
    @Mock private ProductService productService;
    @Mock private UserService    userService;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AdminController adminController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User    testUser;
    private Product testProduct;
    private Order   testOrder;
    private Category testCategory;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("admin@test.com");
        testUser.setFirstName("Admin");
        testUser.setLastName("Impofer");
        testUser.setRole(User.UserRole.ADMIN);
        testUser.setActive(true);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("Taladro Industrial");
        testProduct.setPrice(new BigDecimal("350.00"));
        testProduct.setStock(2);
        testProduct.setMinStock(5);

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setTotal(new BigDecimal("500.00"));
        testOrder.setStatus(Order.OrderStatus.PENDING);
        testOrder.setPaymentStatus(Order.PaymentStatus.PENDING);
        testOrder.setShippingAddress("Av. Lima 123");
        testOrder.setOrderDetails(Collections.emptyList()); // evita NPE en el stream

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Herramientas");
        testCategory.setDescription("Herramientas industriales");
    }

    // ─────────────────────────────────────────────────────
    // GET /api/admin/stats
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/stats → 200 con campos totalSales, totalOrders, totalClients, totalProducts")
    void getStats_returns200WithRequiredFields() throws Exception {
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());

        when(orderService.getTotalRevenue()).thenReturn(new BigDecimal("12500.00"));
        when(orderService.getTotalOrders()).thenReturn(47L);
        when(userService.getAllUsers()).thenReturn(List.of(testUser));
        when(productService.getAllProducts()).thenReturn(List.of(testProduct));
        when(productService.getLowStockProducts()).thenReturn(List.of(testProduct));
        when(orderService.getOrdersPagedAndFiltered(any(), any(), any(), any())).thenReturn(emptyPage);
        when(orderService.getDailySalesStats(7)).thenReturn(new LinkedHashMap<>());

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSales").value(12500.00))
                .andExpect(jsonPath("$.totalOrders").value(47))
                .andExpect(jsonPath("$.totalClients").value(1))
                .andExpect(jsonPath("$.totalProducts").value(1))
                .andExpect(jsonPath("$.lowStockCount").value(1))
                .andExpect(jsonPath("$.recentOrders").isArray())
                .andExpect(jsonPath("$.chartData").exists());
    }

    @Test
    @DisplayName("GET /api/admin/stats → recentOrders contiene las órdenes de la página")
    void getStats_recentOrdersContainsPageContent() throws Exception {
        Page<Order> page = new PageImpl<>(List.of(testOrder));

        when(orderService.getTotalRevenue()).thenReturn(BigDecimal.ZERO);
        when(orderService.getTotalOrders()).thenReturn(1L);
        when(userService.getAllUsers()).thenReturn(Collections.emptyList());
        when(productService.getAllProducts()).thenReturn(Collections.emptyList());
        when(productService.getLowStockProducts()).thenReturn(Collections.emptyList());
        when(orderService.getOrdersPagedAndFiltered(any(), any(), any(), any())).thenReturn(page);
        when(orderService.getDailySalesStats(7)).thenReturn(new LinkedHashMap<>());

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentOrders", hasSize(1)));
    }

    // ─────────────────────────────────────────────────────
    // GET /api/admin/low-stock
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/low-stock → 200 con lista de productos con stock bajo")
    void getLowStockAlerts_returns200WithList() throws Exception {
        when(productService.getLowStockProducts()).thenReturn(List.of(testProduct));

        mockMvc.perform(get("/api/admin/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Taladro Industrial"))
                .andExpect(jsonPath("$[0].stock").value(2));
    }

    @Test
    @DisplayName("GET /api/admin/low-stock → 200 con lista vacía cuando no hay alertas")
    void getLowStockAlerts_emptyList_returns200EmptyArray() throws Exception {
        when(productService.getLowStockProducts()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ─────────────────────────────────────────────────────
    // GET /api/admin/users
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/users → 200 con estructura paginada de usuarios")
    void getAllUsers_returns200WithUserList() throws Exception {
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users[0].email").value("admin@test.com"))
                .andExpect(jsonPath("$.users[0].firstName").value("Admin"))
                .andExpect(jsonPath("$.users[0].role").value("ADMIN"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/users → la respuesta NO expone el campo password")
    void getAllUsers_responseDoesNotExposePassword() throws Exception {
        testUser.setPassword("$2a$12$hashedPassword");
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findAll(any(Pageable.class))).thenReturn(userPage);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].password").doesNotExist());
    }

    // ─────────────────────────────────────────────────────
    // POST /api/admin/users
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/admin/users con datos válidos → 200 con el usuario creado")
    void createUser_validData_returns200() throws Exception {
        User newUser = new User();
        newUser.setEmail("nuevo@test.com");
        newUser.setPassword("Pass1234!");
        newUser.setFirstName("Carlos");
        newUser.setLastName("Rojas");

        when(userService.createUser(any())).thenReturn(testUser);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("POST /api/admin/users sin role → asigna CUSTOMER por defecto")
    void createUser_noRole_defaultsToCustomer() throws Exception {
        User newUser = new User();
        newUser.setEmail("sin-rol@test.com");
        newUser.setPassword("Pass1234!");
        newUser.setFirstName("Maria");
        newUser.setLastName("Lopez");
        // role no establecido → debe asignarse CUSTOMER

        User saved = new User();
        saved.setId(2L);
        saved.setEmail("sin-rol@test.com");
        saved.setRole(User.UserRole.CUSTOMER);

        when(userService.createUser(any())).thenReturn(saved);

        mockMvc.perform(post("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    // ─────────────────────────────────────────────────────
    // DELETE /api/admin/users/{id}
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("DELETE /api/admin/users/{id} → 204 sin contenido")
    void deleteUser_returns204() throws Exception {
        doNothing().when(userService).deleteUser(1L);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isNoContent());

        verify(userService).deleteUser(1L);
    }

    // ─────────────────────────────────────────────────────
    // GET /api/admin/categories
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/categories → 200 con lista de categorías")
    void getAllCategories_returns200WithList() throws Exception {
        when(categoryRepository.findAll()).thenReturn(List.of(testCategory));

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Herramientas"));
    }

    // ─────────────────────────────────────────────────────
    // GET /api/admin/orders (paginado)
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/orders → 200 con estructura paginada (orders, currentPage, totalItems, totalPages)")
    void getAllOrders_returns200WithPaginatedStructure() throws Exception {
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderService.getOrdersPagedAndFiltered(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").isArray())
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/admin/orders → cada orden contiene los campos requeridos")
    void getAllOrders_orderMapContainsRequiredFields() throws Exception {
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderService.getOrdersPagedAndFiltered(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].id").value(1))
                .andExpect(jsonPath("$.orders[0].status").value("PENDING"))
                .andExpect(jsonPath("$.orders[0].paymentStatus").value("PENDING"))
                .andExpect(jsonPath("$.orders[0].orderDetails").isArray())
                .andExpect(jsonPath("$.orders[0].shippingMethod").exists());
    }

    @Test
    @DisplayName("GET /api/admin/orders?search=lima → pasa el término de búsqueda al servicio")
    void getAllOrders_withSearchParam_passesSearchToService() throws Exception {
        Page<Order> emptyPage = new PageImpl<>(Collections.emptyList());
        when(orderService.getOrdersPagedAndFiltered(eq("lima"), any(), any(), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/admin/orders").param("search", "lima"))
                .andExpect(status().isOk());

        verify(orderService).getOrdersPagedAndFiltered(eq("lima"), any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/admin/orders → shippingMethod es RECOJO EN TIENDA cuando la dirección contiene 'recojo'")
    void getAllOrders_recojoAddress_setsPickupMethod() throws Exception {
        testOrder.setShippingAddress("Recojo en tienda – Sede Lima");
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderService.getOrdersPagedAndFiltered(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].shippingMethod").value("RECOJO EN TIENDA"));
    }

    @Test
    @DisplayName("GET /api/admin/orders → shippingMethod es ENVÍO A DOMICILIO para dirección normal")
    void getAllOrders_normalAddress_setsDeliveryMethod() throws Exception {
        testOrder.setShippingAddress("Av. Arequipa 1234, Lima");
        Page<Order> page = new PageImpl<>(List.of(testOrder));
        when(orderService.getOrdersPagedAndFiltered(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[0].shippingMethod").value("ENVÍO A DOMICILIO"));
    }

    // ─────────────────────────────────────────────────────
    // PUT /api/admin/orders/{id}/shipping-status
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/admin/orders/{id}/shipping-status estado válido → 200 con la orden actualizada")
    void updateShippingStatus_validStatus_returns200() throws Exception {
        testOrder.setShippingStatus(Order.ShippingStatus.SHIPPED);
        when(orderService.updateShippingStatus(1L, Order.ShippingStatus.SHIPPED)).thenReturn(testOrder);

        Map<String, String> payload = new HashMap<>();
        payload.put("status", "SHIPPED");

        mockMvc.perform(put("/api/admin/orders/1/shipping-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/admin/orders/{id}/shipping-status orden inexistente → 404")
    void updateShippingStatus_orderNotFound_returns404() throws Exception {
        when(orderService.updateShippingStatus(99L, Order.ShippingStatus.DELIVERED)).thenReturn(null);

        Map<String, String> payload = new HashMap<>();
        payload.put("status", "DELIVERED");

        mockMvc.perform(put("/api/admin/orders/99/shipping-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/admin/orders/{id}/shipping-status estado inválido → 400")
    void updateShippingStatus_invalidStatus_returns400() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("status", "ESTADO_INVENTADO");

        mockMvc.perform(put("/api/admin/orders/1/shipping-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────
    // GET /api/admin/cotizaciones
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/admin/cotizaciones → 200 con lista de cotizaciones")
    void getCotizaciones_returns200WithList() throws Exception {
        testOrder.setStatus(Order.OrderStatus.COTIZACION);
        testOrder.setCotizacionCode("COT-2026-00001");
        testOrder.setUser(testUser);

        when(orderService.getCotizaciones()).thenReturn(List.of(testOrder));

        mockMvc.perform(get("/api/admin/cotizaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("COTIZACION"))
                .andExpect(jsonPath("$[0].cotizacionCode").value("COT-2026-00001"))
                .andExpect(jsonPath("$[0].customer.name").value("Admin Impofer"));
    }

    @Test
    @DisplayName("GET /api/admin/cotizaciones → 200 con lista vacía cuando no hay cotizaciones")
    void getCotizaciones_empty_returns200EmptyArray() throws Exception {
        when(orderService.getCotizaciones()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/cotizaciones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ─────────────────────────────────────────────────────
    // PUT /api/admin/cotizaciones/{id}/confirmar
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/admin/cotizaciones/{id}/confirmar cotización encontrada → 200 con orden en PENDING")
    void confirmarCotizacion_found_returns200() throws Exception {
        testOrder.setStatus(Order.OrderStatus.PENDING);
        when(orderService.confirmarCotizacion(1L)).thenReturn(testOrder);

        mockMvc.perform(put("/api/admin/cotizaciones/1/confirmar"))
                .andExpect(status().isOk());

        verify(orderService).confirmarCotizacion(1L);
    }

    @Test
    @DisplayName("PUT /api/admin/cotizaciones/{id}/confirmar cotización inexistente → 404")
    void confirmarCotizacion_notFound_returns404() throws Exception {
        when(orderService.confirmarCotizacion(99L)).thenReturn(null);

        mockMvc.perform(put("/api/admin/cotizaciones/99/confirmar"))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────────────────────────────────
    // PUT /api/admin/cotizaciones/{id}/ignorar
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/admin/cotizaciones/{id}/ignorar cotización encontrada → 200")
    void ignorarCotizacion_found_returns200() throws Exception {
        testOrder.setStatus(Order.OrderStatus.IGNORED);
        when(orderService.ignorarCotizacion(1L)).thenReturn(testOrder);

        mockMvc.perform(put("/api/admin/cotizaciones/1/ignorar"))
                .andExpect(status().isOk());

        verify(orderService).ignorarCotizacion(1L);
    }

    @Test
    @DisplayName("PUT /api/admin/cotizaciones/{id}/ignorar cotización inexistente → 404")
    void ignorarCotizacion_notFound_returns404() throws Exception {
        when(orderService.ignorarCotizacion(99L)).thenReturn(null);

        mockMvc.perform(put("/api/admin/cotizaciones/99/ignorar"))
                .andExpect(status().isNotFound());
    }
}
