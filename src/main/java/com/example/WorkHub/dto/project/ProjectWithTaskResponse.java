package com.example.WorkHub.dto.project;

import com.example.WorkHub.dto.task.TaskResponse;

import java.time.Instant;
import java.util.List;

public class ProjectWithTaskResponse {

    private Long projectId;
    private String projectName;
    private Long tenantId;
    private Instant createdAt;
    private List<TaskResponse> tasks;

    public ProjectWithTaskResponse(Long projectId, String projectName, Long tenantId,
                                   Instant createdAt, List<TaskResponse> tasks) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.tenantId = tenantId;
        this.createdAt = createdAt;
        this.tasks = tasks;
    }

    public Long getProjectId() { return projectId; }
    public String getProjectName() { return projectName; }
    public Long getTenantId() { return tenantId; }
    public Instant getCreatedAt() { return createdAt; }
    public List<TaskResponse> getTasks() { return tasks; }
}
