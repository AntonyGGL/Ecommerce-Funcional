package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de Caja Blanca — UserService
 * Cubre todas las ramas: createUser, changePassword,
 * generatePasswordResetToken y resetPassword.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — Tests de Caja Blanca")
class UserServiceWhiteBoxTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "resetPasswordUrl",
                "http://localhost:8080/reset-password.html");
    }

    // ─────────────────────────────────────────────────────
    // createUser
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createUser: codifica la contraseña y establece active = true")
    void createUser_encodesPasswordAndSetsActive() {
        User user = new User();
        user.setPassword("rawPassword");
        user.setActive(false);

        when(passwordEncoder.encode("rawPassword")).thenReturn("encodedPassword");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User result = userService.createUser(user);

        assertEquals("encodedPassword", result.getPassword());
        assertTrue(result.getActive());
        verify(passwordEncoder).encode("rawPassword");
        verify(userRepository).save(user);
    }

    // ─────────────────────────────────────────────────────
    // changePassword — rama: usuario no encontrado
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword: lanza excepcion cuando el usuario no existe")
    void changePassword_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.changePassword(99L, "old", "new"));

        assertTrue(ex.getMessage().contains("no encontrado"));
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────
    // generatePasswordResetToken — rama: email no registrado
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("generatePasswordResetToken: retorna sin hacer nada cuando el email no existe")
    void generatePasswordResetToken_unknownEmail_returnsQuietly() {
        when(userRepository.findByEmail("noExiste@test.com")).thenReturn(null);

        assertDoesNotThrow(() -> userService.generatePasswordResetToken("noExiste@test.com"));

        verify(userRepository, never()).save(any());
        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    // ─────────────────────────────────────────────────────
    // generatePasswordResetToken — rama: email registrado
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("generatePasswordResetToken: guarda token y envia correo cuando el email existe")
    void generatePasswordResetToken_knownEmail_savesTokenAndSendsEmail() {
        User user = new User();
        user.setEmail("cliente@test.com");
        user.setFirstName("Maria");

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(user);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.generatePasswordResetToken("cliente@test.com");

        // El token debe haberse generado (no nulo, no vacío)
        assertNotNull(user.getResetPasswordToken());
        assertFalse(user.getResetPasswordToken().isEmpty());

        // La expiracion debe ser aproximadamente +1 hora
        assertNotNull(user.getResetPasswordTokenExpiry());
        assertTrue(user.getResetPasswordTokenExpiry().isAfter(LocalDateTime.now()));

        verify(userRepository).save(user);
        verify(emailService).sendEmail(eq("cliente@test.com"), anyString(), anyString());
    }

    @Test
    @DisplayName("generatePasswordResetToken: el correo incluye el token en la URL")
    void generatePasswordResetToken_emailBodyContainsToken() {
        User user = new User();
        user.setEmail("cliente@test.com");
        user.setFirstName(null); // rama firstName == null

        when(userRepository.findByEmail("cliente@test.com")).thenReturn(user);
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.generatePasswordResetToken("cliente@test.com");

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmail(any(), any(), bodyCaptor.capture());

        String body = bodyCaptor.getValue();
        assertTrue(body.contains(user.getResetPasswordToken()),
                "El cuerpo del correo debe contener el token");
        assertTrue(body.contains("reset-password.html"),
                "El cuerpo del correo debe contener la URL de reset");
    }

    // ─────────────────────────────────────────────────────
    // resetPassword — rama: token null
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword: lanza excepcion cuando el token es null")
    void resetPassword_nullToken_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.resetPassword(null, "newPass"));

        assertTrue(ex.getMessage().contains("obligatorio"));
    }

    // ─────────────────────────────────────────────────────
    // resetPassword — rama: token vacío
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword: lanza excepcion cuando el token es una cadena vacía")
    void resetPassword_emptyToken_throws() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.resetPassword("", "newPass"));

        assertTrue(ex.getMessage().contains("obligatorio"));
    }

    // ─────────────────────────────────────────────────────
    // resetPassword — rama: token no encontrado en BD
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword: lanza excepcion cuando el token no existe en BD")
    void resetPassword_tokenNotInDb_throws() {
        when(userRepository.findByResetPasswordToken("tokenFake")).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.resetPassword("tokenFake", "newPass"));

        assertTrue(ex.getMessage().contains("inválido o expirado"));
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────
    // resetPassword — rama: token expirado
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword: lanza excepcion cuando el token ya expiró")
    void resetPassword_expiredToken_throws() {
        User user = new User();
        user.setResetPasswordToken("tokenExpirado");
        user.setResetPasswordTokenExpiry(LocalDateTime.now().minusHours(2)); // expirado hace 2h

        when(userRepository.findByResetPasswordToken("tokenExpirado")).thenReturn(user);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.resetPassword("tokenExpirado", "newPass"));

        assertTrue(ex.getMessage().contains("inválido o expirado"));
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────
    // resetPassword — rama: token expirado (expiry == null)
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword: lanza excepcion cuando el token existe pero expiry es null")
    void resetPassword_nullExpiry_throws() {
        User user = new User();
        user.setResetPasswordToken("tokenSinExpiry");
        user.setResetPasswordTokenExpiry(null);

        when(userRepository.findByResetPasswordToken("tokenSinExpiry")).thenReturn(user);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.resetPassword("tokenSinExpiry", "newPass"));

        assertTrue(ex.getMessage().contains("inválido o expirado"));
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────
    // resetPassword — rama: token válido y vigente
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword: cambia contraseña, borra token y expiry cuando el token es válido")
    void resetPassword_validToken_updatesPasswordAndClearsToken() {
        User user = new User();
        user.setResetPasswordToken("tokenValido");
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
        user.setPassword("oldEncodedPass");

        when(userRepository.findByResetPasswordToken("tokenValido")).thenReturn(user);
        when(passwordEncoder.encode("nuevaContrasena")).thenReturn("encodedNueva");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        userService.resetPassword("tokenValido", "nuevaContrasena");

        assertEquals("encodedNueva", user.getPassword());
        assertNull(user.getResetPasswordToken(),
                "El token debe borrarse tras el reset");
        assertNull(user.getResetPasswordTokenExpiry(),
                "La expiracion debe borrarse tras el reset");
        verify(userRepository).save(user);
    }
}
