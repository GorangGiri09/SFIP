
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : StartupSession.java
 * Package      : com.sfip.backend.entity
 * File Path    : src/main/java/com/sfip/backend/entity/StartupSession.java
 *
 * Purpose:
 * Represents one startup idea analysis session created by a user.
 *
 * Responsibilities:
 * 1. Stores the startup idea submitted for analysis.
 * 2. Stores ML similarity analysis results.
 * 3. Stores the AI-generated analysis report.
 * 4. Tracks the analysis status.
 * 5. Maintains session creation and update timestamps.
 * 6. Connects the session with its owner (User).
 *
 * Database Table: startup_sessions
 *
 * Relationship:
 * Many StartupSessions can belong to one User.
 *
 * Note:
 * Follow-up chat messages will be stored separately
 * in the ChatMessage entity.
 */

package com.sfip.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "startup_sessions")
public class StartupSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User who created this startup analysis session.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Original startup idea submitted by the user.
    @Column(name = "startup_idea", nullable = false, columnDefinition = "TEXT")
    private String startupIdea;

    // Stores ML similarity analysis output.
    // Format will be finalized when ML integration is implemented.
    @Column(name = "ml_result", columnDefinition = "TEXT")
    private String mlResult;

    // Stores the initial AI-generated analysis report.
    @Column(name = "ai_report", columnDefinition = "TEXT")
    private String aiReport;

    // Current analysis lifecycle status.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Default constructor required by JPA.
    public StartupSession() {
    }

    // Constructor for creating a new analysis session.
    public StartupSession(User user, String startupIdea) {
        this.user = user;
        this.startupIdea = startupIdea;
        this.status = AnalysisStatus.PENDING;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getStartupIdea() {
        return startupIdea;
    }

    public void setStartupIdea(String startupIdea) {
        this.startupIdea = startupIdea;
    }

    public String getMlResult() {
        return mlResult;
    }

    public void setMlResult(String mlResult) {
        this.mlResult = mlResult;
    }

    public String getAiReport() {
        return aiReport;
    }

    public void setAiReport(String aiReport) {
        this.aiReport = aiReport;
    }

    public AnalysisStatus getStatus() {
        return status;
    }

    public void setStatus(AnalysisStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
