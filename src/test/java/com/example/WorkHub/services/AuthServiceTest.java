package com.example.WorkHub.services;

import com.example.WorkHub.config.multitenancy.TenantContext;
import com.example.WorkHub.dtos.UserRegisterRequestDTO;
import com.example.WorkHub.dtos.UserResponseDTO;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.models.User;
import com.example.WorkHub.repository.ProjectRepository;
import com.example.WorkHub.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static javax.management.Query.eq;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
@ExtendWith(SpringExtension.class)
class AuthServiceTest {
   @Mock
   private UserRepository userRepository;
   @Mock
    private JwtService jwtService;
   @Mock
   private ProjectRepository projectRepository;

   @Mock
   private TenantService tenantService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;
//   @InjectMocks
   private AuthService authService;

    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        tenant = mock(Tenant.class);
        when(tenant.getId()).thenReturn(1L);
        when(tenant.getName()).thenReturn("TestTenant");

        user = mock(User.class);
        when(user.getEmail()).thenReturn("test@example.com");
        when(user.getPassword()).thenReturn("encodedPassword");
        when(user.getRole()).thenReturn("USER");
        when(user.getTenantId()).thenReturn(1L);
        authService = new AuthService(tenantService, userRepository, jwtService, passwordEncoder, authenticationManager);
    }

    @AfterEach
    void tearDown() {
        // Ensure TenantContext is always cleared between tests
        TenantContext.clear();
    }
    @Test
    void registerUser_success_returnsUserResponseDTO() {
        UserRegisterRequestDTO request = new UserRegisterRequestDTO(
                "test@example.com", "password123", "USER", "TestTenant"
        );

        when(tenantService.getTenantByName("TestTenant")).thenReturn(tenant);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class), 1L)).thenReturn("jwt-token");

        UserResponseDTO response = authService.registerUser(request);

        assert(response!= null);
        assertEquals("test@example.com", response.email());
        assertEquals("USER",response.role());
        assertEquals("TestTenant",response.tenantName());
        assertEquals("jwt-token",response.token());
    }


//    @Test
//    void loginUser() {
//    }
}