package com.example.WorkHub.services;

import com.example.WorkHub.config.multitenancy.TenantContext;
import com.example.WorkHub.dtos.UserLoginRequestDTO;
import com.example.WorkHub.dtos.UserRegisterRequestDTO;
import com.example.WorkHub.dtos.UserResponseDTO;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.models.User;
import com.example.WorkHub.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
        TenantContext.setTenantId(tenant.getId());
        System.out.println("tenant id: "+ TenantContext.getTenantId());

        try {
            user.setEmail(userRequestDTO.email());
            user.setRole(userRequestDTO.role());
            user.setPassword(passwordEncoder.encode(userRequestDTO.password()));
            user.setTenantId(tenant.getId());
            userRepository.save(user);
            String token = jwtService.generateToken(user, tenant.getId());
            return new UserResponseDTO(user.getEmail(), user.getRole(), tenant.getName(), token);
        } finally {
            TenantContext.clear();
        }
    }

    public String loginUser(UserLoginRequestDTO userLoginRequestDTO) {
        Tenant tenant = tenantService.getTenantByName(userLoginRequestDTO.tenantName().trim());
        TenantContext.setTenantId(tenant.getId());
        System.out.println("tenant id: "+ TenantContext.getTenantId());
        try {
            User user = (User) userRepository.findByEmailAndTenantId(
                    userLoginRequestDTO.email(), tenant.getId()
            ).orElseThrow(() -> new UsernameNotFoundException("User not found"));
            if (!passwordEncoder.matches(userLoginRequestDTO.password(), user.getPassword())) {
                throw new BadCredentialsException("Invalid credentials");
            }

            return jwtService.generateToken(user, tenant.getId());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            TenantContext.clear();
        }
    }
}
