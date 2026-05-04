package com.example.WorkHub.controllers;

import com.example.WorkHub.dto.project.CreateProjectWithTaskRequest;
import com.example.WorkHub.dto.project.ProjectWithTaskResponse;
import com.example.WorkHub.models.ApiError;
import com.example.WorkHub.services.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@Tag(name = "Projects", description = "Transactional project + task operations (Data Integrity demo)")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Operation(
            summary = "Create project with initial task (atomic)",
            description = """
                    Creates a Project and its initial Task in a **single database transaction**.
                    If any step fails, the entire operation is rolled back.

                    Set `simulateTaskFailure = true` to deliberately trigger a failure after the
                    project is persisted — proving the project INSERT is rolled back.""",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Project and task created successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation failed",
                            content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "404", description = "Tenant not found",
                            content = @Content(schema = @Schema(implementation = ApiError.class))),
                    @ApiResponse(responseCode = "409", description = "Transaction rolled back (simulated failure)",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @PostMapping
    public ResponseEntity<ProjectWithTaskResponse> createProjectWithTask(
            @Valid @RequestBody CreateProjectWithTaskRequest request) {
        ProjectWithTaskResponse response = projectService.createProjectWithInitialTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "Get project by ID",
            description = "Returns a project and all its tasks.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Project found"),
                    @ApiResponse(responseCode = "404", description = "Project not found",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ProjectWithTaskResponse> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @Operation(
            summary = "List projects by tenant",
            description = "Returns all projects (with their tasks) belonging to a tenant.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Projects listed"),
                    @ApiResponse(responseCode = "404", description = "Tenant not found",
                            content = @Content(schema = @Schema(implementation = ApiError.class)))
            }
    )
    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<ProjectWithTaskResponse>> getProjectsByTenant(
            @PathVariable Long tenantId) {
        return ResponseEntity.ok(projectService.getProjectsByTenant(tenantId));
    }
}
