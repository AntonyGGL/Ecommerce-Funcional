package com.example.demo.service;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void changePassword_ShouldUpdatePassword_WhenOldPasswordMatches() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setPassword("encodedOldPassword");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPassword", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedNewPassword");
        
        // Act
        userService.changePassword(1L, "oldPassword", "newPassword");
        
        // Assert
        assertEquals("encodedNewPassword", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_ShouldThrowException_WhenOldPasswordDoesNotMatch() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setPassword("encodedOldPassword");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedOldPassword")).thenReturn(false);
        
        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            userService.changePassword(1L, "wrongPassword", "newPassword");
        });
        
        assertEquals("La contraseña actual es incorrecta", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }
}
