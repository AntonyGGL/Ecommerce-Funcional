package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de Caja Negra — AuthController
 * Verifica el contrato HTTP de los endpoints de autenticación:
 * qué código devuelve y qué campos aparecen en la respuesta.
 * No se examina la lógica interna del controlador.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — Tests de Caja Negra")
class AuthControllerBlackBoxTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private User testUser;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("cliente@test.com");
        testUser.setPassword("$2a$10$hashedPasswordHere");
        testUser.setFirstName("Ana");
        testUser.setLastName("García");
        testUser.setRole(User.UserRole.CUSTOMER);
    }

    // ─────────────────────────────────────────────────────
    // POST /api/auth/login
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/login credenciales válidas → 200 con token y email")
    void login_validCredentials_returns200WithTokenAndEmail() throws Exception {
        when(userService.getUserByEmail("cliente@test.com")).thenReturn(testUser);
        when(passwordEncoder.matches("pass123", "$2a$10$hashedPasswordHere")).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyString(), anyString())).thenReturn("jwt.mock.token");

        LoginRequest request = new LoginRequest("cliente@test.com", "pass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.mock.token"))
                .andExpect(jsonPath("$.email").value("cliente@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /api/auth/login contraseña incorrecta → 400")
    void login_wrongPassword_returns400() throws Exception {
        when(userService.getUserByEmail("cliente@test.com")).thenReturn(testUser);
        when(passwordEncoder.matches("wrongpass", "$2a$10$hashedPasswordHere")).thenReturn(false);

        LoginRequest request = new LoginRequest("cliente@test.com", "wrongpass");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login usuario no registrado → 400")
    void login_userNotFound_returns400() throws Exception {
        when(userService.getUserByEmail("noexiste@test.com")).thenReturn(null);

        LoginRequest request = new LoginRequest("noexiste@test.com", "pass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────
    // POST /api/auth/register
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/register email nuevo → 200 con token")
    void register_newEmail_returns200WithToken() throws Exception {
        when(userService.getUserByEmail("nuevo@test.com")).thenReturn(null);
        when(userService.createUser(any())).thenReturn(testUser);
        when(jwtTokenProvider.generateToken(anyString(), anyString())).thenReturn("jwt.new.token");

        User newUser = new User();
        newUser.setEmail("nuevo@test.com");
        newUser.setPassword("pass123");
        newUser.setFirstName("Luis");
        newUser.setLastName("Pérez");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.new.token"))
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    @DisplayName("POST /api/auth/register email ya registrado → 400")
    void register_duplicateEmail_returns400() throws Exception {
        when(userService.getUserByEmail(anyString())).thenReturn(testUser);

        User duplicate = new User();
        duplicate.setEmail("cliente@test.com");
        duplicate.setPassword("pass123");
        duplicate.setFirstName("X");
        duplicate.setLastName("Y");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest());
    }

    // ─────────────────────────────────────────────────────
    // POST /api/auth/validate
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/validate token válido → 200 true")
    void validateToken_valid_returns200True() throws Exception {
        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);

        mockMvc.perform(post("/api/auth/validate")
                        .param("token", "valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("POST /api/auth/validate token inválido → 200 false")
    void validateToken_invalid_returns200False() throws Exception {
        when(jwtTokenProvider.validateToken("expired.token")).thenReturn(false);

        mockMvc.perform(post("/api/auth/validate")
                        .param("token", "expired.token"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    // ─────────────────────────────────────────────────────
    // POST /api/auth/forgot-password
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/forgot-password email registrado → 200 con mensaje genérico")
    void forgotPassword_registeredEmail_returns200WithMessage() throws Exception {
        doNothing().when(userService).generatePasswordResetToken("cliente@test.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .param("email", "cliente@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/auth/forgot-password email desconocido → 200 con el mismo mensaje (no revela si existe)")
    void forgotPassword_unknownEmail_returns200SameMessage() throws Exception {
        // UserService no lanza excepción para emails desconocidos: retorna silenciosamente
        doNothing().when(userService).generatePasswordResetToken("noexiste@test.com");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .param("email", "noexiste@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    // ─────────────────────────────────────────────────────
    // POST /api/auth/reset-password
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/auth/reset-password token válido → 200 con confirmación")
    void resetPassword_validToken_returns200() throws Exception {
        doNothing().when(userService).resetPassword("tokenValido", "NuevaPass123!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .param("token", "tokenValido")
                        .param("newPassword", "NuevaPass123!"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/auth/reset-password token expirado → 400 con mensaje de error")
    void resetPassword_expiredToken_returns400() throws Exception {
        doThrow(new RuntimeException("Token inválido o expirado"))
                .when(userService).resetPassword("tokenExpirado", "NuevaPass123!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .param("token", "tokenExpirado")
                        .param("newPassword", "NuevaPass123!"))
                .andExpect(status().isBadRequest());
    }
}
