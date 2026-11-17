
/*
 * ============================================================
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : StartupFeatures.java
 * Package      : com.sfip.backend.integration.analysis
 * File Path    : src/main/java/com/sfip/backend/integration/analysis/StartupFeatures.java
 *
 * Purpose:
 * Represents structured startup information extracted
 * from the user's original startup description.
 *
 * Responsibilities:
 * 1. Stores startup information extracted by Groq AI.
 * 2. Provides structured inputs for the ML prediction model.
 * 3. Preserves optional information when it is unavailable.
 * 4. Provides a consistent data contract between AI and ML layers.
 *
 * Data Integrity Rules:
 * - Missing information must remain null.
 * - No startup information should be fabricated.
 * - Funding must be represented in USD millions.
 * - Original startup description is preserved separately.
 *
 * ============================================================
 */

package com.sfip.backend.integration.analysis;

import java.math.BigDecimal;

/**
 * Immutable representation of extracted startup information.
 *
 * Used between the Groq extraction layer and ML integration layer.
 */
public record StartupFeatures(

        // Estimated or explicitly provided total funding in USD millions.
        BigDecimal fundingUsdMillions,

        // Country where the startup operates or is based.
        String country,

        // Startup industry, either a known bucket or free text.
        String industry,

        // Customer problem addressed by the startup.
        String problem,

        // Proposed product or service.
        String solution,

        // Intended customer segment.
        String targetCustomers,

        // Revenue generation approach.
        String businessModel

) {

    /**
     * Validates extracted funding information.
     *
     * Funding must be non-negative when provided.
     */
    public StartupFeatures {

        if (fundingUsdMillions != null
                && fundingUsdMillions.signum() < 0) {

            throw new IllegalArgumentException(
                    "Startup funding cannot be negative."
            );
        }

        country = normalize(country);
        industry = normalize(industry);
        problem = normalize(problem);
        solution = normalize(solution);
        targetCustomers = normalize(targetCustomers);
        businessModel = normalize(businessModel);
    }


    /**
     * Converts blank strings into null.
     *
     * This ensures missing information is not treated
     * as valid extracted data.
     */
    private static String normalize(String value) {

        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }


    /**
     * Checks whether all three ML prediction inputs
     * are unavailable.
     *
     * @return true if prediction inputs are insufficient
     */
    public boolean hasNoPredictionInputs() {

        return fundingUsdMillions == null
                && country == null
                && industry == null;
    }
}
