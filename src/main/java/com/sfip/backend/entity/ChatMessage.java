
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : ChatMessage.java
 * Package      : com.sfip.backend.entity
 * File Path    : src/main/java/com/sfip/backend/entity/ChatMessage.java
 *
 * Purpose:
 * Represents a single message exchanged between the user
 * and the AI assistant within a startup analysis session.
 *
 * Responsibilities:
 * 1. Stores the message content.
 * 2. Identifies the sender through MessageRole.
 * 3. Associates each message with its startup session.
 * 4. Maintains the message creation timestamp.
 * 5. Supports continuous chat history.
 *
 * Database Table: chat_messages
 *
 * Relationship:
 * Many ChatMessages belong to one StartupSession.
 */

package com.sfip.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "chat_messages",
    indexes = {
        @Index(
            name = "idx_chat_session_created",
            columnList = "session_id, created_at"
        )
    }
)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Startup session to which this message belongs.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private StartupSession session;

    // Identifies whether the message is from user or AI.
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    // Actual message text.
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Default constructor required by JPA.
    public ChatMessage() {
    }

    // Constructor for creating a chat message.
    public ChatMessage(
            StartupSession session,
            MessageRole role,
            String content) {

        this.session = session;
        this.role = role;
        this.content = content;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public StartupSession getSession() {
        return session;
    }

    public void setSession(StartupSession session) {
        this.session = session;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
