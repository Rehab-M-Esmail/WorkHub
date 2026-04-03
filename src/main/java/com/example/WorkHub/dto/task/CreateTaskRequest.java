package com.example.WorkHub.dto.task;

import jakarta.validation.constraints.NotBlank;

public class CreateTaskRequest {
    @NotBlank(message = "Task title is required")
    private String title;

    void setTitle(String title) {this.title = title;}
    String getTitle() {return title;}
}
