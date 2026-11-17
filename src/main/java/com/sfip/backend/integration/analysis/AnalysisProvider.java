
package com.sfip.backend.integration.analysis;

/**
 * ============================================================================
 * File Name: AnalysisProvider.java
 * Project: Startup Failure Intelligence Platform (SFIP)
 * ----------------------------------------------------------------------------
 * Description:
 * Defines the common contract for startup analysis providers.
 *
 * Responsibilities:
 * 1. Accept the original startup idea for analysis.
 * 2. Return a standardized AnalysisResult.
 * 3. Keep the application independent of provider-specific implementations.
 *
 * Architecture:
 * StartupAnalysisService
 *         |
 *         v
 *   AnalysisProvider
 *         |
 *         v
 *   Groq + ML Provider
 *
 * Notes:
 * - The configured implementation uses Groq for structured feature
 *   extraction and final report generation.
 * - The Python ML service performs startup prediction and similar-startup
 *   retrieval.
 * - Provider-specific orchestration and fallback handling remain inside
 *   the implementation.
 * - This interface does not depend on any particular AI or ML technology.
 * ============================================================================
 *
 * Author: SFIP Development Team
 */
public interface AnalysisProvider {

    /**
     * Analyzes the given startup idea and returns the complete analysis.
     *
     * @param startupIdea the original startup idea submitted by the user
     * @return standardized result containing ML data and AI-generated report
     */
    AnalysisResult analyze(String startupIdea);

}
