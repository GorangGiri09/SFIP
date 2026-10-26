
/*
 * ============================================================
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : ChatMessageController.java
 * Package      : com.sfip.backend.controller
 * File Path    : src/main/java/com/sfip/backend/controller/ChatMessageController.java
 *
 * Purpose:
 * Exposes authenticated APIs for session chat messages.
 *
 * Responsibilities:
 * 1. Processes user messages through ChatService.
 * 2. Retrieves chat history.
 * 3. Verifies authenticated user identity.
 * 4. Enforces session ownership through service-layer checks.
 * 5. Returns safe API responses.
 *
 * ============================================================
 */

package com.sfip.backend.controller;

import com.sfip.backend.entity.ChatMessage;
import com.sfip.backend.entity.User;
import com.sfip.backend.repository.UserRepository;
import com.sfip.backend.service.ChatMessageService;
import com.sfip.backend.service.ChatService;
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

/**
 * REST controller for chat messages associated with startup sessions.
 *
 * All endpoints require an authenticated user.
 *
 * POST processes a user message and returns the saved assistant reply.
 * GET retrieves the conversation history.
 */
@RestController
@RequestMapping("/api/sessions/{sessionId}/messages")
public class ChatMessageController {

    private final ChatService chatService;
    private final ChatMessageService chatMessageService;
    private final StartupSessionService startupSessionService;
    private final UserRepository userRepository;

    /**
     * Constructor injection for chat, session and user services.
     *
     * @param chatService orchestrates user messages and AI replies
     * @param chatMessageService retrieves persisted chat messages
     * @param startupSessionService verifies session ownership
     * @param userRepository resolves the authenticated user
     */
    public ChatMessageController(
            ChatService chatService,
            ChatMessageService chatMessageService,
            StartupSessionService startupSessionService,
            UserRepository userRepository) {

        this.chatService = chatService;
        this.chatMessageService = chatMessageService;
        this.startupSessionService = startupSessionService;
        this.userRepository = userRepository;
    }

    // --------------------------------------------------------
    // POST /api/sessions/{sessionId}/messages
    //
    // Processes a user message through the AI chat workflow.
    // --------------------------------------------------------

    /**
     * Processes a user message and returns the assistant response.
     *
     * The ChatService is responsible for verifying session
     * ownership, saving the user message, generating the AI
     * reply and persisting the assistant response.
     *
     * @param sessionId target startup session ID
     * @param request incoming user message
     * @param authentication current Spring Security authentication
     * @return API response containing the saved assistant message
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> saveUserMessage(
            @PathVariable Long sessionId,
            @Valid @RequestBody CreateMessageRequest request,
            Authentication authentication) {

        // Resolve the currently authenticated user.
        User user = getAuthenticatedUser(authentication);

        // Reject invalid session identifiers.
        validateSessionId(sessionId);

        /*
         * Process the conversation.
         *
         * ChatService handles session ownership verification,
         * user message persistence, AI generation and
         * assistant message persistence.
         */
        ChatMessage assistantMessage = chatService.processMessage(
                user.getId(),
                sessionId,
                request.content()
        );

        // Convert the saved assistant message to a safe response.
        Map<String, Object> messageResponse =
                toMessageResponse(assistantMessage);

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put("message", "AI reply generated successfully.");
        response.put("data", messageResponse);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // --------------------------------------------------------
    // GET /api/sessions/{sessionId}/messages
    //
    // Retrieves chat history in chronological order.
    // --------------------------------------------------------

    /**
     * Retrieves messages belonging to the specified session.
     *
     * @param sessionId target startup session ID
     * @param authentication current Spring Security authentication
     * @return ordered chat history
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getChatHistory(
            @PathVariable Long sessionId,
            Authentication authentication) {

        User user = getAuthenticatedUser(authentication);

        validateSessionId(sessionId);

        // Verify ownership before retrieving chat history.
        verifySessionOwnership(sessionId, user.getId());

        // Retrieve messages and convert entities into safe responses.
        List<Map<String, Object>> messages =
                chatMessageService
                        .getChatHistory(sessionId, user.getId())
                        .stream()
                        .map(this::toMessageResponse)
                        .toList();

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("success", true);
        response.put("sessionId", sessionId);
        response.put("count", messages.size());
        response.put("messages", messages);

        return ResponseEntity.ok(response);
    }

    // --------------------------------------------------------
    // Resolve authenticated user from Spring Security.
    // --------------------------------------------------------

    /**
     * Resolves the authenticated application user using email.
     *
     * The identity is taken from Spring Security rather than
     * from the request body.
     *
     * @param authentication current authentication
     * @return authenticated application user
     */
    private User getAuthenticatedUser(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
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

    // --------------------------------------------------------
    // Validate session ID.
    // --------------------------------------------------------

    /**
     * Validates the session identifier.
     *
     * @param sessionId startup session ID
     */
    private void validateSessionId(Long sessionId) {

        if (sessionId == null || sessionId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid session ID."
            );
        }
    }

    // --------------------------------------------------------
    // Verify session ownership.
    // --------------------------------------------------------

    /**
     * Ensures that the requested session belongs to the user.
     *
     * @param sessionId target session ID
     * @param userId authenticated user ID
     */
    private void verifySessionOwnership(
            Long sessionId,
            Long userId) {

        startupSessionService
                .getUserSession(sessionId, userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Analysis session not found."
                        )
                );
    }

    // --------------------------------------------------------
    // Convert ChatMessage entity into a safe API response.
    // --------------------------------------------------------

    /**
     * Converts a ChatMessage entity into a response map.
     *
     * Only the required message fields are exposed.
     * The complete database entity is never returned directly.
     *
     * @param message saved chat message
     * @return safe message response
     */
    private Map<String, Object> toMessageResponse(
            ChatMessage message) {

        Map<String, Object> item = new LinkedHashMap<>();

        item.put("messageId", message.getId());
        item.put("role", message.getRole());
        item.put("content", message.getContent());
        item.put("createdAt", message.getCreatedAt());

        return item;
    }

    // --------------------------------------------------------
    // Request DTO.
    // --------------------------------------------------------

    /**
     * Request body for submitting a chat message.
     *
     * Content must not be blank and cannot exceed 20,000
     * characters.
     */
    public record CreateMessageRequest(

            @NotBlank(
                    message = "Message content is required."
            )
            @Size(
                    max = 20000,
                    message = "Message cannot exceed 20000 characters."
            )
            String content

    ) {
    }
}
