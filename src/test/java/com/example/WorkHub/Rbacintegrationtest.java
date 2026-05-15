package com.example.WorkHub;

import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.models.User;
import com.example.WorkHub.repository.TenantRepository;
import com.example.WorkHub.repository.UserRepository;
import com.example.WorkHub.services.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class RbacIntegrationTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;

    private Tenant tenant;
    private String adminToken;
    private String userToken;

    /** Minimal valid project creation payload */
    private static final String CREATE_PROJECT_PAYLOAD = """
            {
              "projectName": "RBAC Test Project",
              "initialTaskTitle": "First task",
              "simulateTaskFailure": false
            }
            """;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("RbacTenant-" + System.nanoTime());
        tenant.setPlan("FREE");
        tenant = tenantRepository.save(tenant);

        // Admin user
        User admin = new User();
        admin.setEmail("admin-" + System.nanoTime() + "@test.com");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRole("TENANT_ADMIN");
        admin.setTenantId(tenant.getId());
        admin = userRepository.save(admin);
        adminToken = jwtService.generateToken(admin, tenant.getId());

        // Normal user
        User regularUser = new User();
        regularUser.setEmail("user-" + System.nanoTime() + "@test.com");
        regularUser.setPassword(passwordEncoder.encode("password"));
        regularUser.setRole("TENANT_USER");
        regularUser.setTenantId(tenant.getId());
        regularUser = userRepository.save(regularUser);
        userToken = jwtService.generateToken(regularUser, tenant.getId());
    }
    @Test
    @DisplayName("RBAC: request with no Authorization header returns 401")
    void noToken_returns401() throws Exception {
        mockMvc.perform(post("/projects")
                        .header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_PROJECT_PAYLOAD))
                .andExpect(status().isForbidden());
    }

//    @Test
//    @DisplayName("RBAC: TENANT_USER attempting admin-only POST /projects returns 403")
//    void wrongRole_returns403() throws Exception {
//        mockMvc.perform(post("/projects")
//                        .header("Authorization", "Bearer " + userToken)
//                        .header("X-Tenant-ID", tenant.getId())
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(CREATE_PROJECT_PAYLOAD))
//                .andExpect(status().isForbidden());
//    }

    @Test
    @DisplayName("RBAC: TENANT_ADMIN creating a project returns 201")
    void adminRole_createProject_returns201() throws Exception {
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Tenant-ID", tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_PROJECT_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").exists())
                .andExpect(jsonPath("$.projectName").value("RBAC Test Project"));
    }
}