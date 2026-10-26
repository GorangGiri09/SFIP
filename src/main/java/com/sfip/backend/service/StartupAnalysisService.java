
package com.sfip.backend.service;

import com.sfip.backend.integration.analysis.AnalysisProvider;
import com.sfip.backend.integration.analysis.AnalysisResult;
import org.springframework.stereotype.Service;

/**
 * File Name: StartupAnalysisService.java
 * Project: Startup Failure Intelligence Platform (SFIP)
 *
 * Description:
 * Provides the application-level service for startup
 * idea analysis by delegating analysis requests to
 * the configured AnalysisProvider.
 *
 * Responsibilities:
 * 1. Validate the startup idea.
 * 2. Delegate analysis to the configured provider.
 * 3. Return the standardized AnalysisResult.
 * 4. Keep the application independent of provider-specific
 *    implementation details.
 *
 * Notes:
 * - Currently uses GeminiOnlyProvider through AnalysisProvider.
 * - Future ML integration should not require changes
 *   to this service's analysis contract.
 * - Session management and report persistence are handled
 *   separately by the session workflow.
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
     * Analyzes a startup idea using the configured provider.
     *
     * @param startupIdea the user's startup idea
     * @return standardized startup analysis result
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
