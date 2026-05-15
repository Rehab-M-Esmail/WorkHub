package com.example.WorkHub.observability;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ObservabilityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;
    @Autowired private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("Test Tenant " + System.nanoTime());
        tenant = tenantRepository.save(tenant);

        User admin = new User();
        admin.setEmail("admin-" + System.nanoTime() + "@test.com");
        admin.setPassword(passwordEncoder.encode("password"));
        admin.setRole("TENANT_ADMIN");
        admin.setTenantId(tenant.getId());
        admin = userRepository.save(admin);

        adminToken = jwtService.generateToken(admin, tenant.getId());
    }

    @Test
    @DisplayName("Observability: /actuator/health returns UP")
    void actuatorHealthEndpoints_returnUp() throws Exception {
        mockMvc.perform(get("/actuator/health").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Observability: /actuator/prometheus exposes Micrometer metrics")
    void prometheusEndpoint_exposesMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_")));
    }
}