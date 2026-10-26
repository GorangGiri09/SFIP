
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : ChatMessageRepository.java
 * Package      : com.sfip.backend.repository
 * File Path    : src/main/java/com/sfip/backend/repository/ChatMessageRepository.java
 *
 * Purpose:
 * Provides database access operations for ChatMessage entities.
 *
 * Responsibilities:
 * 1. Saves user and assistant messages.
 * 2. Retrieves chat history for a startup session.
 * 3. Returns messages in chronological order.
 *
 * Database Table: chat_messages
 */

package com.sfip.backend.repository;

import com.sfip.backend.entity.ChatMessage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, Long> {

    // Retrieves messages in chronological order.
    List<ChatMessage> findAllBySession_IdOrderByCreatedAtAscIdAsc(
            Long sessionId
    );
}
