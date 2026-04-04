package com.example.WorkHub.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateProjectWithTaskRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    private String projectName;

    @NotBlank(message = "Initial task title is required")
    @Size(min = 1, max = 255, message = "Task title must be between 1 and 255 characters")
    private String initialTaskTitle;

    @NotNull(message = "Tenant ID is required")
    private Long tenantId;

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
