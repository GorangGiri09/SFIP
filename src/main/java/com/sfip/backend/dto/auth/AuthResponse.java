
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : AuthResponse.java
 * Package      : com.sfip.backend.dto.auth
 * File Path    : src/main/java/com/sfip/backend/dto/auth/AuthResponse.java
 *
 * Purpose:
 * Represents the authentication response returned to the client.
 *
 * Responsibilities:
 * 1. Returns the authenticated user's basic information.
 * 2. Carries the authentication access token.
 * 3. Provides a consistent response structure for auth APIs.
 *
 * Usage:
 * Used by AuthController after successful registration or login.
 *
 * Security:
 * Password and password hash must never be included.
 * The access token is issued only after successful authentication.
 */

package com.sfip.backend.dto.auth;

public record AuthResponse(

        Long userId,
        String name,
        String email,
        String accessToken,
        String tokenType

) {
}
