package com.example.WorkHub.dtos;

import com.example.WorkHub.config.multitenancy.TenantContext;

public record UserResponseDTO(String email, String role, String tenantName, String token) {
}
