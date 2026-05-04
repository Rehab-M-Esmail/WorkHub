package com.example.WorkHub.dto.task;

public class TaskResponse {

    private Long id;
    private String title;
    private String status;
    private Long projectId;

    public TaskResponse(Long id, String title, String status, Long projectId) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.projectId = projectId;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {this.id = id;}
    public String getTitle() {return title;}
    public void setTitle(String title) {this.title = title;}
    public String getStatus() {return status;}
    public void setStatus(String status) {this.status = status;}
    public Long getProjectId() {return projectId;}
    public void setProjectId(Long projectId) {this.projectId = projectId;}

}