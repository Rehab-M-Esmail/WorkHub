package com.example.WorkHub.models;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiFieldError {
    private String field;
    private String message;
}