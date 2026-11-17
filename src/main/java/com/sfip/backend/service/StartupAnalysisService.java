
package com.sfip.backend.service;

import com.sfip.backend.integration.analysis.AnalysisProvider;
import com.sfip.backend.integration.analysis.AnalysisResult;
import org.springframework.stereotype.Service;

/**
 * ============================================================================
 * File Name: StartupAnalysisService.java
 * Project: Startup Failure Intelligence Platform (SFIP)
 * ----------------------------------------------------------------------------
 * Description:
 * Provides the application-level service for analyzing startup ideas.
 *
 * Responsibilities:
 * 1. Validate the startup idea received from the application.
 * 2. Delegate analysis to the configured AnalysisProvider.
 * 3. Return a standardized AnalysisResult containing the ML result
 *    and the final AI-generated report.
 * 4. Keep the application independent of provider-specific implementations.
 *
 * Architecture:
 * Controller
 *     -> StartupSessionService
 *         -> StartupAnalysisService
 *             -> AnalysisProvider
 *                 -> Groq + ML Integration
 *
 * Notes:
 * - Groq-based feature extraction and report generation are handled
 *   by the configured AnalysisProvider implementation.
 * - ML prediction and similar-startup retrieval are orchestrated
 *   by the provider, not by this service.
 * - This service does not directly communicate with external APIs.
 * - Session persistence and status management remain the responsibility
 *   of StartupSessionService.
 * ============================================================================
 *
 * Author: SFIP Development Team
 */
@Service
public class StartupAnalysisService {

    private final AnalysisProvider analysisProvider;

    public StartupAnalysisService(AnalysisProvider analysisProvider) {
        this.analysisProvider = analysisProvider;
    }

    /**
     * Analyzes a startup idea using the configured analysis provider.
     *
     * @param startupIdea original startup idea submitted by the user
     * @return standardized result containing ML data and AI report
     * @throws IllegalArgumentException if the startup idea is empty
     */
    public AnalysisResult analyzeStartup(String startupIdea) {

        if (startupIdea == null || startupIdea.isBlank()) {
            throw new IllegalArgumentException(
                "Startup idea cannot be empty."
            );
        }

        return analysisProvider.analyze(startupIdea.trim());
    }
}
