
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : AuthController.java
 * Package      : com.sfip.backend.controller
 * File Path    : src/main/java/com/sfip/backend/controller/AuthController.java
 *
 * Purpose:
 * Exposes REST endpoints for user registration and login.
 *
 * Responsibilities:
 * 1. Accept registration requests.
 * 2. Accept login requests.
 * 3. Validate incoming request DTOs.
 * 4. Delegate authentication logic to AuthService.
 * 5. Return authentication responses.
 *
 * Security Notes:
 * - Never accept userId or role from the client for authentication.
 * - Passwords are accepted only through request DTOs.
 * - Passwords must never be returned in API responses.
 */

package com.sfip.backend.controller;

import com.sfip.backend.dto.auth.AuthResponse;
import com.sfip.backend.dto.auth.LoginRequest;
import com.sfip.backend.dto.auth.RegisterRequest;
import com.sfip.backend.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Register a new user.
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Login an existing user.
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}
