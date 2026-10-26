
/*
 * ============================================================
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : ChatService.java
 * Package      : com.sfip.backend.service
 * File Path    : src/main/java/com/sfip/backend/service/ChatService.java
 *
 * Purpose:
 * Orchestrates chatbot conversations within startup sessions.
 *
 * Responsibilities:
 * 1. Validates incoming user messages.
 * 2. Verifies startup session ownership.
 * 3. Persists user messages.
 * 4. Generates AI replies through Gemini.
 * 5. Persists assistant responses.
 *
 * ChatMessageService handles message persistence.
 * AiService handles communication with Gemini.
 *
 * ============================================================
 */

package com.sfip.backend.service;

import com.sfip.backend.entity.ChatMessage;
import com.sfip.backend.integration.ai.AiService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service responsible for orchestrating chatbot conversations.
 *
 * This service coordinates session verification, message
 * persistence and AI response generation.
 */
@Service
public class ChatService {

    private final AiService aiService;
    private final ChatMessageService chatMessageService;
    private final StartupSessionService startupSessionService;

    /**
     * Constructs the chat orchestration service.
     *
     * @param aiService service for Gemini API communication
     * @param chatMessageService service for chat message persistence
     * @param startupSessionService service for session ownership
     * verification
     */
    public ChatService(
            AiService aiService,
            ChatMessageService chatMessageService,
            StartupSessionService startupSessionService) {

        this.aiService = aiService;
        this.chatMessageService = chatMessageService;
        this.startupSessionService = startupSessionService;
    }

    /**
     * Processes a user's message within an existing startup session.
     *
     * Workflow:
     * 1. Validate the message.
     * 2. Verify session ownership.
     * 3. Save the user message.
     * 4. Generate the AI reply.
     * 5. Save the assistant response.
     *
     * The session must belong to the authenticated user.
     *
     * @param userId authenticated user's ID
     * @param sessionId startup session ID
     * @param message user's message
     * @return saved assistant ChatMessage entity
     *
     * @throws IllegalArgumentException if the message is empty
     * @throws ResponseStatusException if the session is not found
     *         or is not accessible
     * @throws IllegalStateException if AI generation fails
     */
    @Transactional
    public ChatMessage processMessage(
            Long userId,
            Long sessionId,
            String message) {

        // Validate the incoming message.
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty.");
        }

        /*
         * Verify that the requested session belongs to the user.
         *
         * getUserSession() returns an Optional.
         * An absent session is treated as not found.
         */
        startupSessionService
                .getUserSession(sessionId, userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Analysis session not found."
                        )
                );

        /*
         * Persist the user's message before requesting AI generation.
         *
         * Note:
         * This operation participates in the current transaction
         * if the called service uses the same transaction manager.
         */
        chatMessageService.saveUserMessage(
                sessionId,
                userId,
                message
        );

        /*
         * Generate the assistant response through Gemini.
         *
         * Currently, only the submitted message is sent.
         * Startup analysis and previous conversation context
         * are not included yet.
         */
        String aiReply = aiService.generateReply(message);

        // Persist the assistant's generated response.
        return chatMessageService.saveAssistantMessage(
                sessionId,
                userId,
                aiReply
        );
    }
}
