
/*
 * ============================================================
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : AnalysisResult.java
 * Package      : com.sfip.backend.integration.analysis
 * File Path    : src/main/java/com/sfip/backend/integration/analysis/AnalysisResult.java
 *
 * Purpose:
 * Represents the consolidated result of startup analysis.
 *
 * Responsibilities:
 * 1. Stores the Machine Learning analysis response.
 * 2. Stores the final Groq-generated startup analysis report.
 * 3. Provides a standardized result contract for analysis providers.
 * 4. Supports ML fallback scenarios when the ML service is unavailable.
 *
 * Data Structure:
 * - mlResult: JSON string containing the FastAPI ML response.
 * - report: Final human-readable startup analysis report.
 *
 * Integration Notes:
 * - ML result is stored separately from the final AI report.
 * - A null ML result indicates that ML analysis was unavailable
 *   or was not performed.
 * - The report must clearly disclose any fallback or missing
 *   ML analysis.
 *
 * Compatibility:
 * - The single-argument constructor is retained for compatibility
 *   with the existing Groq-only provider during integration.
 *
 * ============================================================
 */

package com.sfip.backend.integration.analysis;


/**
 * Immutable result of the complete startup analysis workflow.
 *
 * @param mlResult serialized JSON response from the ML service
 * @param report final Groq-generated startup analysis report
 */
public record AnalysisResult(

        String mlResult,

        String report

) {

    /**
     * Validates the analysis result.
     *
     * The final report is mandatory, while the ML result
     * is optional to support fallback scenarios.
     */
    public AnalysisResult {

        if (report == null || report.isBlank()) {
            throw new IllegalArgumentException(
                    "Startup analysis report cannot be empty."
            );
        }

        report = report.trim();

        if (mlResult != null && mlResult.isBlank()) {
            mlResult = null;
        }
    }


    /**
     * Compatibility constructor for the existing
     * Groq-only analysis provider.
     *
     * @param report final AI-generated report
     */
    public AnalysisResult(String report) {

        this(null, report);
    }


    /**
     * Checks whether a valid ML response was stored.
     *
     * @return true if an ML result is available
     */
    public boolean hasMlResult() {

        return mlResult != null;
    }
}
