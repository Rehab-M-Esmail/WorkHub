package com.example.WorkHub.controllers;

import com.example.WorkHub.dtos.UserLoginRequestDTO;
import com.example.WorkHub.dtos.UserRegisterRequestDTO;
import com.example.WorkHub.dtos.UserResponseDTO;
import com.example.WorkHub.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class AuthController {
     AuthService userService;
     public AuthController(AuthService userService){
         this.userService = userService;
     }
    @PostMapping("/signup")
    public UserResponseDTO registerUser(@RequestBody UserRegisterRequestDTO userRegisterRequestDTO){
        return this.userService.registerUser(userRegisterRequestDTO);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> loginUser(@RequestBody UserLoginRequestDTO userLoginRequestDTO){
        String token = this.userService.loginUser(userLoginRequestDTO);
        return ResponseEntity.ok(Map.of("token", token));
    }
}
