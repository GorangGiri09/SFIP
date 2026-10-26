
package com.sfip.backend.integration.analysis;

/**
 * File Name: AnalysisResult.java
 * Project: Startup Failure Intelligence Platform (SFIP)
 *
 * Description:
 * Represents the common result returned by a startup
 * analysis provider, regardless of its implementation.
 *
 * Responsibilities:
 * 1. Hold the final startup analysis report.
 * 2. Provide a stable result contract for the
 *    analysis service.
 * 3. Keep provider-specific response formats
 *    separate from the application layer.
 *
 * Notes:
 * - Used by Gemini-only and future ML-based providers.
 * - Contains the final report intended for storage.
 * - ML-specific data mapping will be handled separately
 *   when the model contract is available.
 *
 * Author: SFIP Development Team
 */
public record AnalysisResult(String report) {

    public AnalysisResult {
        if (report == null || report.isBlank()) {
            throw new IllegalArgumentException(
                "Analysis report cannot be empty."
            );
        }

        report = report.trim();
    }
}
