
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : MessageRole.java
 * Package      : com.sfip.backend.entity
 * File Path    : src/main/java/com/sfip/backend/entity/MessageRole.java
 *
 * Purpose:
 * Defines the supported sender roles for chat messages.
 *
 * Responsibilities:
 * 1. Identifies user messages.
 * 2. Identifies AI assistant messages.
 *
 * Usage:
 * Used by the ChatMessage entity.
 */

package com.sfip.backend.entity;

public enum MessageRole {

    USER,
    ASSISTANT
}
