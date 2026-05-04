package com.example.WorkHub.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 100, message = "Project name must be between 3 and 100 characters")
    private String name;

    public String getName() {return name;}
    public void setName(String name) {this.name = name;}

}