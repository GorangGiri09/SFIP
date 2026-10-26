
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : StartupSessionRepository.java
 * Package      : com.sfip.backend.repository
 * File Path    : src/main/java/com/sfip/backend/repository/StartupSessionRepository.java
 *
 * Purpose:
 * Provides database access operations for StartupSession entities.
 *
 * Responsibilities:
 * 1. Performs CRUD operations on startup sessions.
 * 2. Retrieves sessions belonging to a particular user.
 * 3. Supports the user's startup analysis history.
 *
 * Database Table: startup_sessions
 */

package com.sfip.backend.repository;

import com.sfip.backend.entity.StartupSession;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StartupSessionRepository
        extends JpaRepository<StartupSession, Long> {

    // Retrieves sessions owned by a particular user.
    List<StartupSession> findAllByUser_IdOrderByCreatedAtDesc(Long userId);

    // Retrieves a session only if it belongs to the given user.
    Optional<StartupSession> findByIdAndUser_Id(Long sessionId, Long userId);
}
