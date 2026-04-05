package com.example.WorkHub.controllers;

import com.example.WorkHub.dtos.UserLoginRequestDTO;
import com.example.WorkHub.dtos.UserRegisterRequestDTO;
import com.example.WorkHub.dtos.UserResponseDTO;
import com.example.WorkHub.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
@Tag(name = "Authentication", description = "User registration and login")
public class AuthController {
     AuthService userService;
     public AuthController(AuthService userService){
         this.userService = userService;
     }

    @Operation(summary = "Register a new user", responses = {
            @ApiResponse(responseCode = "200", description = "User registered successfully")
    })
    @PostMapping("/signup")
    public UserResponseDTO registerUser(@RequestBody UserRegisterRequestDTO userRegisterRequestDTO){
        return this.userService.registerUser(userRegisterRequestDTO);
    }

    @Operation(summary = "Login and get JWT token", responses = {
            @ApiResponse(responseCode = "200", description = "Login successful, token returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody UserLoginRequestDTO userLoginRequestDTO){
        String token = this.userService.loginUser(userLoginRequestDTO);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
