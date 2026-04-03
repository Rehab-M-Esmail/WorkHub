package com.example.WorkHub.services;

import com.example.WorkHub.dtos.UserLoginRequestDTO;
import com.example.WorkHub.dtos.UserRegisterRequestDTO;
import com.example.WorkHub.dtos.UserResponseDTO;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.models.User;
import com.example.WorkHub.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    TenantService tenantService;
    UserRepository userRepository;
    JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    AuthenticationManager authenticationManager;

    public AuthService(TenantService tenantService, UserRepository userRepository, JwtService jwtService, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager){
        this.tenantService = tenantService;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public UserResponseDTO registerUser(UserRegisterRequestDTO userRequestDTO){
        User user = new User();
        Tenant tenant;
        tenant = this.tenantService.getTenantByName(userRequestDTO.tenantName().trim());
        System.out.println("tenant name;"+ tenant.getName());
        user.setEmail(userRequestDTO.email());
        user.setTenant(tenant);
        user.setRole(userRequestDTO.role());
        user.setPassword(passwordEncoder.encode(userRequestDTO.password()));
        this.userRepository.save(user);
        String token = this.jwtService.generateToken(user);
        return new UserResponseDTO(user.getEmail(), user.getRole(), user.getTenant(),token);
    }

    public String loginUser(UserLoginRequestDTO userLoginRequestDTO) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userLoginRequestDTO.email(), userLoginRequestDTO.password())
        );
        User user = userRepository.findByEmail(userLoginRequestDTO.email()).orElseThrow();
        return jwtService.generateToken(user);
    }
}
