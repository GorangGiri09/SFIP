
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : LoginRequest.java
 * Package      : com.sfip.backend.dto.auth
 * File Path    : src/main/java/com/sfip/backend/dto/auth/LoginRequest.java
 *
 * Purpose:
 * Represents the incoming login request from the client.
 *
 * Responsibilities:
 * 1. Accepts the user's email and password.
 * 2. Validates the incoming login data.
 * 3. Transfers login input to the authentication service.
 *
 * Usage:
 * Used by AuthController during user login.
 *
 * Security:
 * The password is verified against the stored password hash.
 * It must never be returned in any API response.
 */

package com.sfip.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(max = 256, message = "Password is too long")
    private String password;

    // Default constructor
    public LoginRequest() {
    }

    // Getters and setters

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
