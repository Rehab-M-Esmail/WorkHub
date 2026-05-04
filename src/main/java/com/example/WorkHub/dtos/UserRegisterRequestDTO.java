package com.example.WorkHub.dtos;

public record UserRegisterRequestDTO(String email, String password, String role, String tenantName) {
    public String getEmail()
    {
        return email;
    }
    String getPassword()
    {
        return password;
    }
    String getRole()
    {
        return role;
    }
    String getTenantName()
    {
        return tenantName;
    }
}
