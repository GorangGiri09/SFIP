
/*
 * ============================================================
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : StartupSessionController.java
 * Package      : com.sfip.backend.controller
 * File Path    : src/main/java/com/sfip/backend/controller/StartupSessionController.java
 *
 * Purpose:
 * Exposes authenticated APIs for startup analysis sessions.
 *
 * Responsibilities:
 * 1. Creates new startup analysis sessions.
 * 2. Triggers startup analysis.
 * 3. Retrieves the authenticated user's analysis history.
 * 4. Retrieves individual analysis session details.
 * 5. Enforces session ownership.
 * 6. Returns safe and structured API responses.
 *
 * Security Notes:
 * - All endpoints require authentication.
 * - User identity is obtained from Spring Security.
 * - Client-supplied user IDs are never trusted.
 * - Password information is never exposed.
 * ============================================================
 */

package com.sfip.backend.controller;

import com.sfip.backend.entity.StartupSession;
import com.sfip.backend.entity.User;
import com.sfip.backend.repository.UserRepository;
import com.sfip.backend.service.StartupSessionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class StartupSessionController {

    private final StartupSessionService startupSessionService;
    private final UserRepository userRepository;

    // Constructor injection.
    public StartupSessionController(
            StartupSessionService startupSessionService,
            UserRepository userRepository) {

        this.startupSessionService = startupSessionService;
        this.userRepository = userRepository;
    }

    // --------------------------------------------------------
    // POST /api/sessions
    //
    // Creates a new startup analysis session.
    // Triggers analysis using the configured provider.
    // --------------------------------------------------------

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        // Step 1: Create session with PENDING status.
        StartupSession session =
                startupSessionService.createSession(
                        user,
                        request.startupIdea()
                );

        // Step 2: Process analysis.
        // This currently runs synchronously.
        startupSessionService.processAnalysis(
                session.getId(),
                user.getId()
        );

        // Step 3: Fetch the latest persisted session.
        StartupSession updatedSession =
                startupSessionService
                        .getUserSession(session.getId(), user.getId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Analysis session not found."
                                )
                        );

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put(
                "message",
                "Startup analysis completed successfully."
        );
        response.put("session", toDetailsResponse(updatedSession));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // --------------------------------------------------------
    // GET /api/sessions
    //
    // Retrieves the authenticated user's analysis history.
    // Latest sessions appear first.
    // --------------------------------------------------------

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAnalysisHistory(
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        List<Map<String, Object>> sessions =
                startupSessionService
                        .getUserAnalysisHistory(user.getId())
                        .stream()
                        .map(this::toHistoryResponse)
                        .toList();

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put("count", sessions.size());
        response.put("sessions", sessions);

        return ResponseEntity.ok(response);
    }

    // --------------------------------------------------------
    // GET /api/sessions/{sessionId}
    //
    // Retrieves one session only if it belongs to the user.
    // --------------------------------------------------------

    @GetMapping("/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSessionDetails(
            @PathVariable Long sessionId,
            Authentication authentication) {

        if (sessionId == null || sessionId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid session ID."
            );
        }

        User user = getAuthenticatedUser(authentication);

        StartupSession session = startupSessionService
                .getUserSession(sessionId, user.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Analysis session not found."
                        )
                );

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put("session", toDetailsResponse(session));

        return ResponseEntity.ok(response);
    }

    // --------------------------------------------------------
    // Resolve authenticated user from Spring Security.
    // --------------------------------------------------------

    private User getAuthenticatedUser(Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                !(authentication.getPrincipal() instanceof UserDetails)) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required."
            );
        }

        UserDetails principal =
                (UserDetails) authentication.getPrincipal();

        String email = principal.getUsername();

        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Authenticated user not found."
                        )
                );
    }

    // --------------------------------------------------------
    // Convert session to history response.
    // --------------------------------------------------------

    private Map<String, Object> toHistoryResponse(
            StartupSession session) {

        Map<String, Object> item = new LinkedHashMap<>();

        item.put("sessionId", session.getId());
        item.put("startupIdea", session.getStartupIdea());
        item.put("status", session.getStatus());
        item.put("createdAt", session.getCreatedAt());
        item.put("updatedAt", session.getUpdatedAt());

        return item;
    }

    // --------------------------------------------------------
    // Convert session to detailed response.
    // --------------------------------------------------------

    private Map<String, Object> toDetailsResponse(
            StartupSession session) {

        Map<String, Object> item = toHistoryResponse(session);

        item.put("mlResult", session.getMlResult());
        item.put("aiReport", session.getAiReport());

        return item;
    }

    // --------------------------------------------------------
    // Request DTO for session creation.
    // --------------------------------------------------------

    public record CreateSessionRequest(

            @NotBlank(message = "Startup idea is required.")

            @Size(
                    min = 20,
                    max = 10000,
                    message = "Startup idea must contain 20 to 10000 characters."
            )

            String startupIdea

    ) {
    }
}
