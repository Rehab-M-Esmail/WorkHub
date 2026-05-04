package com.example.WorkHub.dtos;

public record UserLoginRequestDTO(String email, String password) {
    public String getEmail() {
        return email;
    }
}
