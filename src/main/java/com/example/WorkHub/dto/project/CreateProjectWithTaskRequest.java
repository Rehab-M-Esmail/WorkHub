package com.example.WorkHub.dto.project;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a project and its initial task atomically")
public class CreateProjectWithTaskRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    @Schema(description = "Name of the project", example = "Alpha")
    private String projectName;

    @NotBlank(message = "Initial task title is required")
    @Size(min = 1, max = 255, message = "Task title must be between 1 and 255 characters")
    @Schema(description = "Title for the initial task", example = "Setup CI pipeline")
    private String initialTaskTitle;

    @NotNull(message = "Tenant ID is required")
    @Schema(description = "ID of the tenant that owns this project", example = "1")
    private Long tenantId;

    @Schema(description = "Set to true to simulate a failure after project creation, triggering a full rollback",
            example = "false")
    private boolean simulateTaskFailure;

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getInitialTaskTitle() { return initialTaskTitle; }
    public void setInitialTaskTitle(String initialTaskTitle) { this.initialTaskTitle = initialTaskTitle; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }

    public boolean isSimulateTaskFailure() { return simulateTaskFailure; }
    public void setSimulateTaskFailure(boolean simulateTaskFailure) { this.simulateTaskFailure = simulateTaskFailure; }
}
