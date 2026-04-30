package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${app.reset-password.url:http://localhost:8080/reset-password.html}")
    private String resetPasswordUrl;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setActive(true);
        return userRepository.save(user);
    }

    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public void generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            log.warn("Solicitud de recuperacion para email no registrado");
            return;
        }
        
        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordTokenExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        // Enviar el correo real
        String resetUrl = resetPasswordUrl + "?token=" + token;
        String subject = "Recuperación de Contraseña - Impofer";
        String body = "Hola " + (user.getFirstName() != null ? user.getFirstName() : "") + ",\n\n" +
                     "Has solicitado restablecer tu contraseña.\n\n" +
                     "Haz clic en el siguiente enlace para crear una nueva contraseña:\n" +
                     resetUrl + "\n\n" +
                     "Este enlace expirará en 1 hora.\n" +
                     "Si no solicitaste este cambio, puedes ignorar este correo.";
        
        emailService.sendEmail(email, subject, body);
    }

    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isEmpty()) {
            throw new RuntimeException("El token es obligatorio");
        }

        User user = userRepository.findByResetPasswordToken(token);
        
        if (user == null || user.getResetPasswordTokenExpiry() == null || 
            user.getResetPasswordTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token inválido o expirado");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordTokenExpiry(null);
        userRepository.save(user);
    }
}
