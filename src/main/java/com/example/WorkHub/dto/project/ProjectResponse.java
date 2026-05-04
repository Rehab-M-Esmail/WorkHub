package com.example.WorkHub.dto.project;

public class ProjectResponse {
    private Long id;
    private String name;
    private String createdBy;

    public ProjectResponse(Long id, String name, String createdBy) {
        this.id = id;
        this.name = name;
        this.createdBy = createdBy;
    }
    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getName() {return name;}
}
