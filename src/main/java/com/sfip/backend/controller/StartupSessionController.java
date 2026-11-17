
/*
 * ============================================================================
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : StartupSessionController.java
 * Package      : com.sfip.backend.controller
 * File Path    : src/main/java/com/sfip/backend/controller/StartupSessionController.java
 * ----------------------------------------------------------------------------
 * Purpose:
 * Exposes authenticated REST APIs for startup analysis sessions.
 *
 * Responsibilities:
 * 1. Creates new startup analysis sessions.
 * 2. Triggers the complete Groq + ML analysis workflow.
 * 3. Retrieves the authenticated user's analysis history.
 * 4. Retrieves individual analysis session details.
 * 5. Enforces session ownership.
 * 6. Returns structured responses compatible with the frontend.
 *
 * Security:
 * - Endpoints require authentication through Spring Security.
 * - User identity is obtained from the authenticated principal.
 * - Client-supplied user IDs are never trusted.
 * - Session ownership is enforced by StartupSessionService.
 *
 * Integration:
 * Controller -> StartupSessionService -> StartupAnalysisService
 *            -> AnalysisProvider -> Groq + FastAPI ML Service
 *
 * Notes:
 * - Analysis currently executes synchronously.
 * - Successful creation returns the final persisted analysis result.
 * - Failed analysis is not returned as a successful completed session.
 * ============================================================================
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class StartupSessionController {

    private final StartupSessionService startupSessionService;
    private final UserRepository userRepository;

    public StartupSessionController(
            StartupSessionService startupSessionService,
            UserRepository userRepository) {

        this.startupSessionService = startupSessionService;
        this.userRepository = userRepository;
    }

    /**
     * Creates a session and executes its startup analysis.
     *
     * POST /api/sessions
     *
     * Request:
     * {
     *     "startupIdea": "..."
     * }
     *
     * Returns the completed session using the existing
     * top-level frontend response contract.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createSession(
            @Valid @RequestBody CreateSessionRequest request,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        // Create a new session with PENDING status.
        StartupSession session =
                startupSessionService.createSession(
                        user,
                        request.startupIdea()
                );

        // Execute the complete analysis workflow.
        startupSessionService.processAnalysis(
                session.getId(),
                user.getId()
        );

        // Fetch the latest persisted session after processing.
        StartupSession updatedSession =
                startupSessionService
                        .getUserSession(session.getId(), user.getId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Analysis session not found."
                                )
                        );

        if (updatedSession.getStatus() !=
                com.sfip.backend.entity.AnalysisStatus.COMPLETED) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Startup analysis did not complete successfully."
            );
        }

        Map<String, Object> response =
                toDetailsResponse(updatedSession);

        response.put("success", true);
        response.put(
                "message",
                "Startup analysis completed successfully."
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Retrieves the authenticated user's analysis history.
     *
     * GET /api/sessions
     */
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

    /**
     * Retrieves a session only if it belongs to the authenticated user.
     *
     * GET /api/sessions/{sessionId}
     */
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

        StartupSession session =
                startupSessionService
                        .getUserSession(sessionId, user.getId())
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Analysis session not found."
                                )
                        );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("success", true);
        response.put("session", toDetailsResponse(session));

        return ResponseEntity.ok(response);
    }

    /**
     * Resolves the authenticated user using Spring Security.
     */
    private User getAuthenticatedUser(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                !(authentication.getPrincipal()
                        instanceof UserDetails)) {

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

    /**
     * Converts a session into its history representation.
     */
    private Map<String, Object> toHistoryResponse(
            StartupSession session) {

        Map<String, Object> item =
                new LinkedHashMap<>();

        item.put("sessionId", session.getId());
        item.put("startupIdea", session.getStartupIdea());
        item.put("status", session.getStatus());
        item.put("createdAt", session.getCreatedAt());
        item.put("updatedAt", session.getUpdatedAt());

        return item;
    }

    /**
     * Converts a session into its detailed representation.
     */
    private Map<String, Object> toDetailsResponse(
            StartupSession session) {

        Map<String, Object> item =
                toHistoryResponse(session);

        item.put("mlResult", session.getMlResult());
        item.put("aiReport", session.getAiReport());

        return item;
    }

    /**
     * Request DTO for creating a startup analysis session.
     */
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
