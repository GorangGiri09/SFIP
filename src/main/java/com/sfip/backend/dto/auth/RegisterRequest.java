
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : RegisterRequest.java
 * Package      : com.sfip.backend.dto.auth
 * File Path    : src/main/java/com/sfip/backend/dto/auth/RegisterRequest.java
 *
 * Purpose:
 * Represents the incoming registration request from the client.
 *
 * Responsibilities:
 * 1. Accepts the user's name, email and password.
 * 2. Validates the incoming registration data.
 * 3. Transfers registration input to the authentication service.
 *
 * Usage:
 * Used by AuthController during user registration.
 *
 * Security:
 * Password is accepted only as request input.
 * It must be encoded before being stored in the database.
 */

package com.sfip.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    public RegisterRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
