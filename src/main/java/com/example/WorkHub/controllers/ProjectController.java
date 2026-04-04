package com.example.WorkHub.controllers;

import com.example.WorkHub.dto.project.CreateProjectWithTaskRequest;
import com.example.WorkHub.dto.project.ProjectWithTaskResponse;
import com.example.WorkHub.services.ProjectService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    /**
     * Creates a project AND its initial task in a single atomic transaction.
     *
     * Set {@code simulateTaskFailure = true} in the request body to trigger a
     * deliberate failure after the project is persisted but before the task is
     * saved — proving that the project INSERT is rolled back.
     */
    @PostMapping
    public ResponseEntity<ProjectWithTaskResponse> createProjectWithTask(
            @Valid @RequestBody CreateProjectWithTaskRequest request) {
        ProjectWithTaskResponse response = projectService.createProjectWithInitialTask(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectWithTaskResponse> getProject(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<ProjectWithTaskResponse>> getProjectsByTenant(
            @PathVariable Long tenantId) {
        return ResponseEntity.ok(projectService.getProjectsByTenant(tenantId));
    }
}
