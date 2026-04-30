package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BlackBoxIntegrationTests {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    @Test
    public void testUserLifecycle_RegisterLoginAndBrowse() throws Exception {
        String baseUrl = "http://localhost:" + port;

        // 1. Registro
        User testUser = new User();
        testUser.setEmail("newuser" + System.currentTimeMillis() + "@example.com");
        testUser.setPassword("password123");
        testUser.setFirstName("New");
        testUser.setLastName("User");
        testUser.setRole(User.UserRole.CUSTOMER);

        HttpRequest registerRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(testUser)))
                .build();

        HttpResponse<String> registerResponse = httpClient.send(registerRequest, HttpResponse.BodyHandlers.ofString());
        
        assertEquals(HttpStatus.OK.value(), registerResponse.statusCode());
        LoginResponse registerBody = objectMapper.readValue(registerResponse.body(), LoginResponse.class);
        assertNotNull(registerBody.getToken());

        String email = registerBody.getEmail();
        String token = registerBody.getToken();

        // 2. Login
        LoginRequest loginReq = new LoginRequest(email, "password123");
        HttpRequest loginRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(loginReq)))
                .build();

        HttpResponse<String> loginResponse = httpClient.send(loginRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpStatus.OK.value(), loginResponse.statusCode());
        LoginResponse loginBody = objectMapper.readValue(loginResponse.body(), LoginResponse.class);
        assertNotNull(loginBody.getToken());

        // 3. Ver productos
        HttpRequest productsRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/products"))
                .GET()
                .build();

        HttpResponse<String> productsResponse = httpClient.send(productsRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpStatus.OK.value(), productsResponse.statusCode());

        // 4. Validar token
        HttpRequest validateRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/validate?token=" + token))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> validateResponse = httpClient.send(validateRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpStatus.OK.value(), validateResponse.statusCode());
        assertEquals("true", validateResponse.body());
    }

    @Test
    public void testLoginFailure() throws Exception {
        String baseUrl = "http://localhost:" + port;
        LoginRequest loginRequest = new LoginRequest("wrong@example.com", "wrongpass");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(loginRequest)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
    }
}
