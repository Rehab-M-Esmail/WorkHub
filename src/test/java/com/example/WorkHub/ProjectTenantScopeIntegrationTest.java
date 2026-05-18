package com.example.WorkHub;

import com.example.WorkHub.support.PostgresIntegrationTestBase;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.models.User;
import com.example.WorkHub.repository.TenantRepository;
import com.example.WorkHub.repository.UserRepository;
import com.example.WorkHub.services.JwtService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
class ProjectTenantScopeIntegrationTest extends PostgresIntegrationTestBase {

    private static final String CREATE_PROJECT_PAYLOAD = """
            {
              "projectName": "Scope Test Project",
              "initialTaskTitle": "First task",
              "simulateTaskFailure": false
            }
            """;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    JwtService jwtService;
    @Autowired
    PasswordEncoder passwordEncoder;

    private Tenant tenantA;
    private Tenant tenantB;
    private String tenantAToken;

    @BeforeEach
    void setUp() {
        tenantA = new Tenant();
        tenantA.setName("ScopeTenantA-" + System.nanoTime());
        tenantA.setPlan("FREE");
        tenantA = tenantRepository.save(tenantA);

        tenantB = new Tenant();
        tenantB.setName("ScopeTenantB-" + System.nanoTime());
        tenantB.setPlan("FREE");
        tenantB = tenantRepository.save(tenantB);

        User adminA = new User();
        adminA.setEmail("scope-admin-" + System.nanoTime() + "@test.com");
        adminA.setPassword(passwordEncoder.encode("password"));
        adminA.setRole("TENANT_ADMIN");
        adminA.setTenantId(tenantA.getId());
        adminA = userRepository.save(adminA);

        tenantAToken = jwtService.generateToken(adminA, tenantA.getId());
    }

    @Test
    @DisplayName("POST /projects: response tenantId matches JWT / X-Tenant-ID")
    void createProject_tenantIdMatchesToken() throws Exception {
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + tenantAToken)
                        .header("X-Tenant-ID", tenantA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_PROJECT_PAYLOAD))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantA.getId()));
    }

    @Test
    @DisplayName("X-Tenant-ID that does not match JWT tenantId returns 403")
    void mismatchedHeader_returns403() throws Exception {
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + tenantAToken)
                        .header("X-Tenant-ID", tenantB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_PROJECT_PAYLOAD))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /projects/tenant lists only projects for the token tenant")
    void listProjects_respectsTenantScope() throws Exception {
        mockMvc.perform(post("/projects")
                        .header("Authorization", "Bearer " + tenantAToken)
                        .header("X-Tenant-ID", tenantA.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_PROJECT_PAYLOAD))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/projects/tenant")
                        .header("Authorization", "Bearer " + tenantAToken)
                        .header("X-Tenant-ID", tenantA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].projectName").value("Scope Test Project"))
                .andExpect(jsonPath("$[0].tenantId").value(tenantA.getId()));
    }
}
