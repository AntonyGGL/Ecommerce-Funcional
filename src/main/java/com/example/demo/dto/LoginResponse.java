package com.example.demo.dto;

public class LoginResponse {
    private String token;
    private String email;
    private String role;
    private String firstName;
    private Long userId;

    public LoginResponse() {
    }

    public LoginResponse(String token, String email, String role, String firstName, Long userId) {
        this.token = token;
        this.email = email;
        this.role = role;
        this.firstName = firstName;
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
