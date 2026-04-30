package com.example.demo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de Caja Blanca — JwtTokenProvider
 * Cubre todas las ramas: generateToken, validateToken,
 * getEmailFromToken (token valido, invalido, manipulado).
 *
 * La clave secreta debe tener al menos 64 bytes para HS512.
 */
@DisplayName("JwtTokenProvider — Tests de Caja Blanca")
class JwtTokenProviderWhiteBoxTest {

    private JwtTokenProvider jwtTokenProvider;

    // 64 caracteres ASCII = 64 bytes: suficiente para HS512
    private static final String TEST_SECRET =
            "ClaveSecretaDeTestParaHS512SoloParaPruebasNoUsarEnProduccion1234";
    private static final long EXPIRATION_MS = 3_600_000L; // 1 hora

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", EXPIRATION_MS);
    }

    // ─────────────────────────────────────────────────────
    // generateToken
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken: genera un token no nulo con tres segmentos JWT (header.payload.signature)")
    void generateToken_returnsValidJwtFormat() {
        String token = jwtTokenProvider.generateToken("usuario@test.com", "CUSTOMER");

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length,
                "Un JWT debe tener exactamente 3 partes separadas por '.'");
    }

    @Test
    @DisplayName("generateToken: tokens distintos para emails distintos")
    void generateToken_differentEmailsProduceDifferentTokens() {
        String t1 = jwtTokenProvider.generateToken("a@test.com", "CUSTOMER");
        String t2 = jwtTokenProvider.generateToken("b@test.com", "CUSTOMER");

        assertNotEquals(t1, t2);
    }

    @Test
    @DisplayName("generateToken: tokens distintos para roles distintos")
    void generateToken_differentRolesProduceDifferentTokens() {
        String t1 = jwtTokenProvider.generateToken("a@test.com", "CUSTOMER");
        String t2 = jwtTokenProvider.generateToken("a@test.com", "ADMIN");

        assertNotEquals(t1, t2);
    }

    // ─────────────────────────────────────────────────────
    // validateToken — rama: token válido
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken: retorna true para un token recién generado")
    void validateToken_freshToken_returnsTrue() {
        String token = jwtTokenProvider.generateToken("admin@test.com", "ADMIN");

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    // ─────────────────────────────────────────────────────
    // validateToken — rama: token manipulado
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken: retorna false para un token con firma manipulada")
    void validateToken_tamperedSignature_returnsFalse() {
        String token = jwtTokenProvider.generateToken("admin@test.com", "ADMIN");
        // Reemplazar el segmento de firma completo con basura para invalidar HMAC
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".firmaCompletamenteInvalidaXXXXXXXX";

        assertFalse(jwtTokenProvider.validateToken(tampered));
    }

    // ─────────────────────────────────────────────────────
    // validateToken — rama: token basura
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken: retorna false para una cadena que no es JWT")
    void validateToken_randomString_returnsFalse() {
        assertFalse(jwtTokenProvider.validateToken("esto.no.es.un.token"));
        assertFalse(jwtTokenProvider.validateToken("completamenteInvalido"));
    }

    // ─────────────────────────────────────────────────────
    // validateToken — rama: token expirado
    //   Inyectamos expiracion = -1 ms para generar un token ya vencido
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("validateToken: retorna false para un token expirado")
    void validateToken_expiredToken_returnsFalse() {
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", -1L);
        String expiredToken = jwtTokenProvider.generateToken("old@test.com", "CUSTOMER");

        // Restaurar para no afectar otros tests
        ReflectionTestUtils.setField(jwtTokenProvider, "expiration", EXPIRATION_MS);

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
    }

    // ─────────────────────────────────────────────────────
    // getEmailFromToken — rama: token válido
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getEmailFromToken: retorna el email correcto para un token valido")
    void getEmailFromToken_validToken_returnsEmail() {
        String token = jwtTokenProvider.generateToken("maria@test.com", "CUSTOMER");

        String email = jwtTokenProvider.getEmailFromToken(token);

        assertEquals("maria@test.com", email);
    }

    // ─────────────────────────────────────────────────────
    // getEmailFromToken — rama: token manipulado
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getEmailFromToken: retorna null para un token con firma manipulada")
    void getEmailFromToken_tamperedToken_returnsNull() {
        String token = jwtTokenProvider.generateToken("hack@test.com", "ADMIN");
        // Reemplazar el segmento de firma completo con basura
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".firmaCompletamenteInvalidaXXXXXXXX";

        assertNull(jwtTokenProvider.getEmailFromToken(tampered));
    }

    // ─────────────────────────────────────────────────────
    // getEmailFromToken — rama: cadena inválida
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getEmailFromToken: retorna null para una cadena que no es JWT")
    void getEmailFromToken_invalidString_returnsNull() {
        assertNull(jwtTokenProvider.getEmailFromToken("no-es-un-token"));
    }

    // ─────────────────────────────────────────────────────
    // Consistencia entre generate y getEmail
    // ─────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken→getEmailFromToken: round-trip produce el email original")
    void roundTrip_generateAndExtract_emailMatches() {
        String[] emails = {"admin@impofer.com", "cliente@test.pe", "user+tag@mail.com"};

        for (String email : emails) {
            String token = jwtTokenProvider.generateToken(email, "CUSTOMER");
            assertEquals(email, jwtTokenProvider.getEmailFromToken(token),
                    "Round-trip fallido para: " + email);
        }
    }
}
