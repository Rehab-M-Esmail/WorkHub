package com.example.WorkHub.dto.task;

import jakarta.validation.constraints.NotBlank;

public class UpdateTaskRequest {

    @NotBlank(message = "Status is required")
    private String status;

    void setStatus(String status) {this.status = status;}
    String getStatus() {return status;}
}
