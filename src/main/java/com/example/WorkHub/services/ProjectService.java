package com.example.WorkHub.services;

import com.example.WorkHub.config.multitenancy.TenantContext;
import com.example.WorkHub.dto.project.CreateProjectWithTaskRequest;
import com.example.WorkHub.dto.project.ProjectWithTaskResponse;
import com.example.WorkHub.dto.task.TaskResponse;
import com.example.WorkHub.exception.ResourceNotFoundException;
import com.example.WorkHub.exception.TaskCreationException;
import com.example.WorkHub.models.Project;
import com.example.WorkHub.models.Task;
import com.example.WorkHub.models.Tenant;
import com.example.WorkHub.repository.ProjectRepository;
import com.example.WorkHub.repository.TaskRepository;
import com.example.WorkHub.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final TenantRepository tenantRepository;
    private final EntityManager entityManager;

    public ProjectService(ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          TenantRepository tenantRepository, EntityManager entityManager) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.tenantRepository = tenantRepository;
        this.entityManager = entityManager;
    }
    private void applyTenantFilter() {
        Session session = entityManager.unwrap(Session.class);
        session.enableFilter("tenantFilter")
                .setParameter("tenantId", TenantContext.getTenantId());
    }
    /**
     * TRANSACTIONAL MULTI-STEP OPERATION
     *
     * This method performs two database writes inside a single transaction:
     *   Step 1 — INSERT the Project row
     *   Step 2 — INSERT the initial Task row linked to that Project
     *
     * If Step 2 fails (either organically or via the simulateTaskFailure flag),
     * Spring rolls back the entire transaction, so the Project from Step 1 is
     * never committed.
     *
     * The {@code simulateTaskFailure} flag on the request DTO exists solely to
     * let callers prove the rollback behaviour without having to craft a real
     * database error.
     */
    @Transactional
    public ProjectWithTaskResponse createProjectWithInitialTask(CreateProjectWithTaskRequest request) {
        applyTenantFilter();
        // ── Step 1: Create and persist the Project ──────────────────────
        // tenant_id is auto-set via @PrePersist from TenantContext
        Project project = new Project();
        project.setName(request.getProjectName());
        project = projectRepository.save(project);
        projectRepository.flush();

        log.info("TX Step 1 COMPLETE — Project '{}' persisted with id={}",
                project.getName(), project.getId());

        // ── Rollback trigger (for demonstration only) ───────────────────
        if (request.isSimulateTaskFailure()) {
            throw new TaskCreationException(
                    "Simulated failure — project id=" + project.getId() + " rolled back.");
        }

        // ── Step 2: Create and persist the initial Task ─────────────────
        Task task = new Task();
        task.setTitle(request.getInitialTaskTitle());
        task.setStatus("TODO");
        task.setProject(project);
        task = taskRepository.save(task);

        log.info("TX Step 2 COMPLETE — Task '{}' persisted with id={} under project id={}",
                task.getTitle(), task.getId(), project.getId());

        // ── Build response ──────────────────────────────────────────────
        TaskResponse taskResponse = new TaskResponse(
                task.getId(), task.getTitle(), task.getStatus(), project.getId());

        return new ProjectWithTaskResponse(
                project.getId(),
                project.getName(),
                project.getTenantId(),
                project.getCreatedAt(),
                List.of(taskResponse));
    }

    @Transactional(readOnly = true)
    public ProjectWithTaskResponse getProjectById(Long projectId) {
        applyTenantFilter();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId));

        List<TaskResponse> taskResponses = project.getTasks().stream()
                .map(t -> new TaskResponse(t.getId(), t.getTitle(), t.getStatus(), project.getId()))
                .toList();

        return new ProjectWithTaskResponse(
                project.getId(),
                project.getName(),
                project.getTenantId(),
                project.getCreatedAt(),
                taskResponses);
    }

    @Transactional(readOnly = true)
    public List<ProjectWithTaskResponse> getProjectsByTenant() {
        applyTenantFilter();
        return projectRepository.findAll().stream()
                .map(project -> {
                    List<TaskResponse> taskResponses = project.getTasks().stream()
                            .map(t -> new TaskResponse(
                                    t.getId(), t.getTitle(), t.getStatus(), project.getId()))
                            .toList();
                    return new ProjectWithTaskResponse(
                            project.getId(),
                            project.getName(),
                            project.getTenantId(),
                            project.getCreatedAt(),
                            taskResponses);
                })
                .toList();
    }
}
