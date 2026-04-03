package com.example.WorkHub.dtos;

public record UserRegisterRequestDTO(String email, String password, String role, String tenantName) {
}
