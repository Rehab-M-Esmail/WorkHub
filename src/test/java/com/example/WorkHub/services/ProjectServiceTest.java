package com.example.WorkHub.services;

import static org.junit.jupiter.api.Assertions.*;

import com.example.WorkHub.support.PostgresIntegrationTestBase;
import com.example.WorkHub.config.multitenancy.TenantContext;
import com.example.WorkHub.dto.project.CreateProjectWithTaskRequest;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.models.User;
import com.example.WorkHub.repository.ProjectRepository;
import com.example.WorkHub.repository.TenantRepository;
import com.example.WorkHub.repository.UserRepository;
import com.example.WorkHub.services.JwtService;
import com.example.WorkHub.exception.TaskCreationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class TransactionRollbackIntegrationTest extends PostgresIntegrationTestBase {

    @Autowired ProjectService projectService;
    @Autowired ProjectRepository projectRepository;
    @Autowired TenantRepository tenantRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtService jwtService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setName("RollbackTenant-" + System.nanoTime());
        tenant.setPlan("FREE");
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setEmail("rollback-" + System.nanoTime() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole("TENANT_ADMIN");
        user.setTenantId(tenant.getId());
        userRepository.save(user);

        TenantContext.setTenantId(tenant.getId());
    }

    @Test
    @DisplayName("Transaction rollback: project INSERT is rolled back when task creation fails — no partial writes")
    void simulatedTaskFailure_rollsBackProjectInsert() {
        long countBefore = projectRepository.findByTenantId(tenant.getId()).size();

        CreateProjectWithTaskRequest request = new CreateProjectWithTaskRequest();
        request.setProjectName("RollbackTestProject");
        request.setInitialTaskTitle("Irrelevant — will never be created");
        request.setSimulateTaskFailure(true);

        assertThatThrownBy(() -> projectService.createProjectWithInitialTask(request))
                .isInstanceOf(TaskCreationException.class)
                .hasMessageContaining("Simulated failure");

        long countAfter = projectRepository.findByTenantId(tenant.getId()).size();

        assertThat(countAfter)
                .as("Project INSERT must be rolled back; no partial write should remain in DB")
                .isEqualTo(countBefore);

        // ── Extra safety: confirm no project with that name exists in the db
        assertThat(projectRepository.findByNameAndTenantId("RollbackTestProject", tenant.getId()))
                .as("No project named 'RollbackTestProject' should exist after rollback")
                .isEmpty();
    }
}