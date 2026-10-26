
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : AuthService.java
 * Package      : com.sfip.backend.service
 * File Path    : src/main/java/com/sfip/backend/service/AuthService.java
 *
 * Purpose:
 * Implements user registration and login business logic.
 *
 * Responsibilities:
 * 1. Register new users.
 * 2. Normalize and validate email addresses.
 * 3. Prevent duplicate email registration.
 * 4. Hash passwords using BCrypt.
 * 5. Authenticate users through Spring Security.
 * 6. Generate JWT access tokens after successful authentication.
 * 7. Return safe authentication response DTOs.
 *
 * Security Notes:
 * - Never store raw passwords.
 * - Never return User entities directly.
 * - Do not reveal whether an email exists during login.
 * - Enforce BCrypt's 72-byte password limit.
 */

package com.sfip.backend.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sfip.backend.dto.auth.AuthResponse;
import com.sfip.backend.dto.auth.LoginRequest;
import com.sfip.backend.dto.auth.RegisterRequest;
import com.sfip.backend.entity.User;
import com.sfip.backend.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    // Register a new user.
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        String name = request.getName().trim();
        String email = normalizeEmail(request.getEmail());
        String rawPassword = request.getPassword();

        validatePassword(rawPassword);

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists"
            );
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);

        User user = new User(name, email, encodedPassword);

        User savedUser = userRepository.saveAndFlush(user);

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(savedUser.getEmail())
                        .password(savedUser.getPassword())
                        .authorities("ROLE_USER")
                        .build();

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getName(),
                savedUser.getEmail(),
                token,
                "Bearer"
        );
    }

    // Authenticate an existing user.
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {

        String email = normalizeEmail(request.getEmail());
        String rawPassword = request.getPassword();

        Authentication authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken
                                .unauthenticated(email, rawPassword)
                );

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found"
                        )
                );

        String token = jwtService.generateToken(userDetails);

        return new AuthResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                token,
                "Bearer"
        );
    }

    // Normalize email consistently.
    private String normalizeEmail(String email) {

        return email.trim().toLowerCase(Locale.ROOT);
    }

    // BCrypt input must not exceed 72 UTF-8 bytes.
    private void validatePassword(String password) {

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException(
                    "Password is required"
            );
        }

        int passwordBytes =
                password.getBytes(StandardCharsets.UTF_8).length;

        if (passwordBytes > 72) {
            throw new IllegalArgumentException(
                    "Password must not exceed 72 UTF-8 bytes"
            );
        }
    }
}
