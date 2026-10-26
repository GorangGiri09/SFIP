
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : AnalysisStatus.java
 * Package      : com.sfip.backend.entity
 * File Path    : src/main/java/com/sfip/backend/entity/AnalysisStatus.java
 *
 * Purpose:
 * Defines the possible lifecycle states of a startup analysis.
 *
 * Responsibilities:
 * 1. Represents whether analysis is pending, processing,
 *    completed, or failed.
 * 2. Provides consistent status values across the backend.
 *
 * Usage:
 * Used by the StartupSession entity.
 */

package com.sfip.backend.entity;

public enum AnalysisStatus {

    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
