package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.model.User;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    @Autowired
    private UserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.getUserByEmail(request.getEmail());
        
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().build();
        }

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().toString());
        LoginResponse response = new LoginResponse(token, user.getEmail(), user.getRole().name(), user.getFirstName(), user.getId());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody User user) {
        User existingUser = userService.getUserByEmail(user.getEmail());
        if (existingUser != null) {
            return ResponseEntity.badRequest().build();
        }

        User newUser = userService.createUser(user);
        String token = jwtTokenProvider.generateToken(newUser.getEmail(), newUser.getRole().toString());
        LoginResponse response = new LoginResponse(token, newUser.getEmail(), newUser.getRole().name(), newUser.getFirstName(), newUser.getId());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate")
    public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
        return ResponseEntity.ok(jwtTokenProvider.validateToken(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        try {
            userService.generatePasswordResetToken(email);
            
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("message", "Si tu correo está registrado, recibirás un enlace de recuperación pronto.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Error en recuperacion de contrasena: {}", e.getMessage());
            return ResponseEntity.ok(java.util.Map.of("message", "Si tu correo está registrado, recibirás un enlace de recuperación pronto."));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        try {
            userService.resetPassword(token, newPassword);
            return ResponseEntity.ok("Contraseña restablecida con éxito");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
