package com.example.WorkHub;

import com.example.WorkHub.dtos.TenantRequestDTO;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class TenantRateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String freeTenantToken;
    private Long freeTenantId;

    private String premiumTenantToken;
    private Long premiumTenantId;

    @BeforeEach
    void setUp() {
        Tenant freeTenant = new Tenant();
        freeTenant.setName("Free Corp " + System.nanoTime());
        freeTenant.setPlan("FREE");
        freeTenant = tenantRepository.save(freeTenant);
        freeTenantId = freeTenant.getId();

        User freeUser = new User();
        freeUser.setEmail("free-user-" + System.nanoTime() + "@test.com");
        freeUser.setPassword(passwordEncoder.encode("password"));
        freeUser.setRole("TENANT_ADMIN");
        freeUser.setTenantId(freeTenantId);
        userRepository.save(freeUser);

        freeTenantToken = jwtService.generateToken(freeUser, freeTenantId);

        Tenant premiumTenant = new Tenant();
        premiumTenant.setName("Premium Corp " + System.nanoTime());
        premiumTenant.setPlan("PREMIUM");
        premiumTenant = tenantRepository.save(premiumTenant);
        premiumTenantId = premiumTenant.getId();

        User premiumUser = new User();
        premiumUser.setEmail("premium-user-" + System.nanoTime() + "@test.com");
        premiumUser.setPassword(passwordEncoder.encode("password"));
        premiumUser.setRole("TENANT_ADMIN");
        premiumUser.setTenantId(premiumTenantId);
        userRepository.save(premiumUser);

        premiumTenantToken = jwtService.generateToken(premiumUser, premiumTenantId);
    }

    @Test
    @DisplayName("Rate Limiting: Free Tier tenant gets 429 after 10 rapid requests")
    void freeTier_shouldThrottle_afterTenRequests() throws Exception {
        // Execute 10 successful requests (Matches the Free Tier limit)
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/tenant/" + freeTenantId)
                            .header("Authorization", "Bearer " + freeTenantToken)
                            .requestAttr("X-Tenant-ID", String.valueOf(freeTenantId))
                            .requestAttr("X-Tenant-Plan", "FREE"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }

        // The 11th rapid request must fail with 429 Too Many Requests
        mockMvc.perform(get("/tenant/" + freeTenantId)
                        .header("Authorization", "Bearer " + freeTenantToken)
                        .requestAttr("X-Tenant-ID", String.valueOf(freeTenantId))
                        .requestAttr("X-Tenant-Plan", "FREE"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("Rate Limiting: Premium Tier tenant bypasses Free limit up to 100 requests")
    void premiumTier_shouldAllowMoreRequests_thanFreeTier() throws Exception {
        // Execute 15 rapid requests (which would fail a Free Tier bucket)
        for (int i = 0; i < 15; i++) {
            mockMvc.perform(get("/tenant/" + premiumTenantId)
                            .header("Authorization", "Bearer " + premiumTenantToken)
                            .requestAttr("X-Tenant-ID", String.valueOf(premiumTenantId))
                            .requestAttr("X-Tenant-Plan", "PREMIUM"))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Rate Limiting: Tenants maintain isolated, separate token buckets")
    void rateLimiting_shouldBeIsolatedPerTenant() throws Exception {
        // Exhaust the Free Tenant's bucket entirely
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/tenant/" + freeTenantId)
                    .header("Authorization", "Bearer " + freeTenantToken)
                    .requestAttr("X-Tenant-ID", String.valueOf(freeTenantId))
                    .requestAttr("X-Tenant-Plan", "FREE"));
        }

        // Verify Free Tenant is locked out
        mockMvc.perform(get("/tenant/" + freeTenantId)
                        .header("Authorization", "Bearer " + freeTenantToken)
                        .requestAttr("X-Tenant-ID", String.valueOf(freeTenantId))
                        .requestAttr("X-Tenant-Plan", "FREE"))
                .andExpect(status().isTooManyRequests());

        // Verify the Premium Tenant can still complete requests
        mockMvc.perform(get("/tenant/" + premiumTenantId)
                        .header("Authorization", "Bearer " + premiumTenantToken)
                        .requestAttr("X-Tenant-ID", String.valueOf(premiumTenantId))
                        .requestAttr("X-Tenant-Plan", "PREMIUM"))
                .andExpect(status().isOk());
    }
}