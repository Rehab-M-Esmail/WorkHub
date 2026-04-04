package com.example.WorkHub.services;

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

    public ProjectService(ProjectRepository projectRepository,
                          TaskRepository taskRepository,
                          TenantRepository tenantRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.tenantRepository = tenantRepository;
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

        // ── Step 0: Resolve the tenant ──────────────────────────────────
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tenant not found with id: " + request.getTenantId()));

        // ── Step 1: Create and persist the Project ──────────────────────
        Project project = new Project();
        project.setName(request.getProjectName());
        project.setTenant(tenant);
        project = projectRepository.save(project);
        projectRepository.flush();

        log.info("TX Step 1 COMPLETE — Project '{}' persisted with id={} (not yet committed)",
                project.getName(), project.getId());

        // ── Rollback trigger (for demonstration only) ───────────────────
        if (request.isSimulateTaskFailure()) {
            log.warn("TX ROLLBACK TRIGGERED — simulateTaskFailure=true. "
                    + "Project id={} will be rolled back.", project.getId());
            throw new TaskCreationException(
                    "Simulated failure during task creation. "
                    + "The project (id=" + project.getId() + ") has been rolled back — "
                    + "it will NOT appear in the database.");
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
                tenant.getId(),
                project.getCreatedAt(),
                List.of(taskResponse));
    }

    @Transactional(readOnly = true)
    public ProjectWithTaskResponse getProjectById(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project not found with id: " + projectId));

        List<TaskResponse> taskResponses = project.getTasks().stream()
                .map(t -> new TaskResponse(t.getId(), t.getTitle(), t.getStatus(), project.getId()))
                .toList();

        return new ProjectWithTaskResponse(
                project.getId(),
                project.getName(),
                project.getTenant().getId(),
                project.getCreatedAt(),
                taskResponses);
    }

    @Transactional(readOnly = true)
    public List<ProjectWithTaskResponse> getProjectsByTenant(Long tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResourceNotFoundException("Tenant not found with id: " + tenantId);
        }

        return projectRepository.findByTenantId(tenantId).stream()
                .map(project -> {
                    List<TaskResponse> taskResponses = project.getTasks().stream()
                            .map(t -> new TaskResponse(
                                    t.getId(), t.getTitle(), t.getStatus(), project.getId()))
                            .toList();
                    return new ProjectWithTaskResponse(
                            project.getId(),
                            project.getName(),
                            project.getTenant().getId(),
                            project.getCreatedAt(),
                            taskResponses);
                })
                .toList();
    }
}
