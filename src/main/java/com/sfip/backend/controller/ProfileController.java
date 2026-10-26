
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : ProfileController.java
 * Package      : com.sfip.backend.controller
 * File Path    : src/main/java/com/sfip/backend/controller/ProfileController.java
 *
 * Purpose:
 * Provides the authenticated user's basic profile information.
 *
 * Responsibilities:
 * 1. Expose a protected profile endpoint.
 * 2. Retrieve the authenticated user's identity.
 * 3. Return safe profile information.
 * 4. Help verify JWT authentication.
 *
 * Security Notes:
 * - This endpoint requires authentication.
 * - User identity is obtained from Spring Security.
 * - Never trust a userId supplied by the client for identity.
 * - Never expose passwords or password hashes.
 */

package com.sfip.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sfip.backend.entity.User;
import com.sfip.backend.repository.UserRepository;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            Authentication authentication) {

        UserDetails principal =
                (UserDetails) authentication.getPrincipal();

        String email = principal.getUsername();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found"
                        )
                );

        Map<String, Object> response = Map.of(
                "success", true,
                "userId", user.getId(),
                "name", user.getName(),
                "email", user.getEmail()
        );

        return ResponseEntity.ok(response);
    }
}
