package com.example.WorkHub.dtos;

public record UserResponseDTO(String email, String role, com.example.WorkHub.models.Tenant tenantName, String token) {
}
