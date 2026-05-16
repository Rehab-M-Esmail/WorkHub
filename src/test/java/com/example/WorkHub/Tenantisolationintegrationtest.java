package com.example.WorkHub;
import com.example.WorkHub.config.multitenancy.TenantContext;
import com.example.WorkHub.models.Project;
import com.example.WorkHub.models.Task;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.models.User;
import com.example.WorkHub.repository.ProjectRepository;
import com.example.WorkHub.repository.TaskRepository;
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

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional          // each test rolls back; DB is clean for the next one
class TenantIsolationIntegrationTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired TaskRepository taskRepository;
    @Autowired JwtService jwtService;
    @Autowired PasswordEncoder passwordEncoder;

    private Tenant tenantA;
    private Tenant tenantB;
    private User   userA;
    private User   userB;
    private Project projectB;
    private Task    taskB;
    private String  tokenA;

    @BeforeEach
    void setUp() {
        tenantA = new Tenant();
        tenantA.setName("TenantA-Isolation-" + System.nanoTime());
        tenantA.setPlan("FREE");
        tenantA = tenantRepository.save(tenantA);

        userA = new User();
        userA.setEmail("userA-" + System.nanoTime() + "@test.com");
        userA.setPassword(passwordEncoder.encode("password"));
        userA.setRole("TENANT_USER");
        userA.setTenantId(tenantA.getId());
        userA = userRepository.save(userA);
        tokenA = jwtService.generateToken(userA, tenantA.getId());

        tenantB = new Tenant();
        tenantB.setName("TenantB-Isolation-" + System.nanoTime());
        tenantB.setPlan("FREE");
        tenantB = tenantRepository.save(tenantB);

        userB = new User();
        userB.setEmail("userB-" + System.nanoTime() + "@test.com");
        userB.setPassword(passwordEncoder.encode("password"));
        userB.setRole("TENANT_USER");
        userB.setTenantId(tenantB.getId());
        userB = userRepository.save(userB);

        TenantContext.setTenantId(tenantB.getId());
        try {
            projectB = new Project();
            projectB.setName("SecretProject-B");
            projectB.setTenantId(tenantB.getId());
            projectB = projectRepository.save(projectB);

            taskB = new Task();
            taskB.setTitle("SecretTask-B");
            taskB.setStatus("TODO");
            taskB.setProject(projectB);
            taskB.setTenantId(tenantB.getId());
            taskB = taskRepository.save(taskB);
        } finally {
            TenantContext.clear();
        }
    }
    @Test
    @DisplayName("Cross-tenant read: GET /projects/{id} owned by Tenant B returns 404 for Tenant A")
    void crossTenantRead_projectOwnedByOtherTenant_returns404() throws Exception {

        mockMvc.perform(get("/projects/{id}", projectB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Tenant-ID", tenantB.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-tenant update: PATCH /tasks/{id} owned by Tenant B returns 404 for Tenant A")
    void crossTenantUpdate_taskOwnedByOtherTenant_returns404() throws Exception {
        String updatePayload = """
                { "status": "DONE" }
                """;

        mockMvc.perform(patch("/tasks/{id}", taskB.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Tenant-ID", tenantB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cross-tenant list: GET /projects/tenant returns empty list for Tenant A (no Tenant B data leaks)")
    void crossTenantList_tenantASeesNoTenantBProjects() throws Exception {

        mockMvc.perform(get("/projects/tenant")
                        .header("Authorization", "Bearer " + tokenA)
                        .header("X-Tenant-ID", tenantA.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").value(empty()));
    }
}