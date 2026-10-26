
package com.sfip.backend.integration.analysis;

/**
 * File Name: AnalysisProvider.java
 * Project: Startup Failure Intelligence Platform (SFIP)
 *
 * Description:
 * Defines the common contract for startup analysis
 * providers used by the application.
 *
 * Responsibilities:
 * 1. Accept a startup idea for analysis.
 * 2. Return a standardized AnalysisResult.
 * 3. Keep the application independent of the
 *    underlying analysis provider.
 *
 * Notes:
 * - The current implementation will use Gemini AI.
 * - A future implementation may integrate the ML model.
 * - Provider-specific logic must remain inside
 *   its respective implementation.
 *
 * Author: SFIP Development Team
 */
public interface AnalysisProvider {

    /**
     * Analyzes the given startup idea and returns
     * the generated startup analysis report.
     *
     * @param startupIdea the user's startup idea
     * @return standardized analysis result
     */
    AnalysisResult analyze(String startupIdea);
}
