
/*
 * ============================================================
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : StartupFeatureExtractor.java
 * Package      : com.sfip.backend.integration.analysis
 * File Path    : src/main/java/com/sfip/backend/integration/analysis/StartupFeatureExtractor.java
 *
 * Purpose:
 * Extracts structured startup information from the user's
 * original startup description using Groq AI.
 *
 * Responsibilities:
 * 1. Accepts the original startup description.
 * 2. Constructs a structured extraction prompt.
 * 3. Delegates AI communication to AiService.
 * 4. Parses the generated JSON response.
 * 5. Converts extracted information into StartupFeatures.
 * 6. Preserves missing information as null.
 * 7. Rejects malformed or invalid AI responses.
 *
 * Extracted Information:
 * - Funding in USD millions
 * - Country
 * - Industry
 * - Problem
 * - Solution
 * - Target customers
 * - Business model
 *
 * Data Integrity Rules:
 * - Information must come from the user's description.
 * - Missing information must remain null.
 * - Funding must not be guessed.
 * - Non-USD funding is not converted without a verified
 *   exchange rate.
 * - AI-generated assumptions are not accepted as facts.
 *
 * Integration:
 * - AI communication: AiService
 * - Output model: StartupFeatures
 * - JSON parsing: Jackson ObjectMapper
 *
 * ============================================================
 */

package com.sfip.backend.integration.analysis;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

import com.sfip.backend.integration.ai.AiService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


/**
 * Extracts structured information from startup descriptions
 * using Groq AI.
 *
 * This class does not perform startup risk prediction or
 * generate the final startup analysis report.
 */
@Component
public class StartupFeatureExtractor {

    private static final Logger logger =
            LoggerFactory.getLogger(StartupFeatureExtractor.class);

    private final AiService aiService;
    private final ObjectMapper objectMapper;


    // --------------------------------------------------------
    // Constructor
    // --------------------------------------------------------

    /**
     * Initializes the extractor with AI and JSON services.
     *
     * @param aiService Groq communication service
     * @param objectMapper Jackson JSON parser
     */
    public StartupFeatureExtractor(
            AiService aiService,
            ObjectMapper objectMapper) {

        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }


    // --------------------------------------------------------
    // Extract Startup Features
    // --------------------------------------------------------

    /**
     * Extracts structured startup information from the
     * original user-provided description.
     *
     * @param startupIdea original startup description
     * @return structured startup information
     */
    public StartupFeatures extract(String startupIdea) {

        if (startupIdea == null || startupIdea.isBlank()) {
            throw new IllegalArgumentException(
                    "Startup description cannot be empty."
            );
        }

        if (startupIdea.length() > 10000) {
            throw new IllegalArgumentException(
                    "Startup description cannot exceed 10000 characters."
            );
        }

        String prompt = buildExtractionPrompt(startupIdea.trim());

        try {

            String generatedResponse =
                    aiService.generateReply(prompt);

            JsonNode root = objectMapper.readTree(
                    cleanJsonResponse(generatedResponse)
            );

            return parseFeatures(root);

        } catch (Exception exception) {

            logger.error(
                    "Startup feature extraction failed. ExceptionType={}",
                    exception.getClass().getSimpleName(),
                    exception
            );

            throw new IllegalStateException(
                    "Unable to extract structured startup information.",
                    exception
            );
        }
    }


    // --------------------------------------------------------
    // Extraction Prompt
    // --------------------------------------------------------

    /**
     * Constructs a strict extraction prompt.
     *
     * Groq is instructed to extract only information
     * explicitly supported by the original description.
     *
     * @param startupIdea original startup description
     * @return structured extraction prompt
     */
    private String buildExtractionPrompt(String startupIdea) {

        return """
                You are a structured information extraction engine
                for the SFIP Startup Failure Intelligence Platform.

                Your task is to extract startup information from
                the user's original startup description.

                STRICT EXTRACTION RULES:

                1. Extract only information explicitly stated or
                   directly supported by the description.

                2. Never invent funding, country, industry,
                   customers, problems, solutions or revenue models.

                3. If information is missing or ambiguous, return null.

                4. Do not assume a startup's country from its language,
                   currency, name or technology.

                5. Funding must be represented as a numeric amount
                   in USD millions.

                6. Only return funding when the description provides
                   an unambiguous USD amount.

                7. Do not convert INR or other currencies into USD
                   without a verified exchange rate.

                8. Do not confuse revenue, valuation, investment
                   targets or projected funding with funding raised.

                9. Industry should be returned as the most appropriate
                   category when supported by the description.

                Accepted industry categories:

                fintech
                health
                edu
                mobility_hw
                commerce_logistics
                software_ai
                media_social
                other

                10. If an industry cannot be determined reliably,
                    return null.

                11. Do not provide explanations, recommendations,
                    analysis or additional commentary.

                OUTPUT FORMAT:

                Return exactly one valid JSON object.

                Use these exact field names:

                {
                  "fundingUsdMillions": null,
                  "country": null,
                  "industry": null,
                  "problem": null,
                  "solution": null,
                  "targetCustomers": null,
                  "businessModel": null
                }

                Every field must be present.

                Use JSON null for unavailable information.
                Do not use strings such as "unknown", "N/A"
                or "not provided" for missing values.

                USER'S STARTUP DESCRIPTION:

                <startup_description>
                """ + startupIdea + """
                </startup_description>

                Return only the JSON object.
                """;
    }


    // --------------------------------------------------------
    // JSON Response Cleaning
    // --------------------------------------------------------

    /**
     * Removes optional Markdown JSON code fences from
     * the generated response.
     *
     * @param response raw Groq response
     * @return cleaned JSON string
     */
    private String cleanJsonResponse(String response) {

        if (response == null || response.isBlank()) {
            throw new IllegalStateException(
                    "Groq returned an empty extraction response."
            );
        }

        String cleaned = response.trim();

        if (cleaned.startsWith("```")) {

            int firstNewLine = cleaned.indexOf('\n');

            if (firstNewLine < 0) {
                throw new IllegalStateException(
                        "Invalid JSON code fence in Groq response."
                );
            }

            cleaned = cleaned.substring(firstNewLine + 1).trim();

            if (!cleaned.endsWith("```")) {
                throw new IllegalStateException(
                        "Incomplete JSON code fence in Groq response."
                );
            }

            cleaned = cleaned.substring(
                    0,
                    cleaned.length() - 3
            ).trim();
        }

        if (!cleaned.startsWith("{") || !cleaned.endsWith("}")) {
            throw new IllegalStateException(
                    "Groq response does not contain a JSON object."
            );
        }

        return cleaned;
    }


    // --------------------------------------------------------
    // Parse Structured Features
    // --------------------------------------------------------

    /**
     * Converts the JSON response into StartupFeatures.
     *
     * @param root parsed JSON object
     * @return validated StartupFeatures instance
     */
    private StartupFeatures parseFeatures(JsonNode root) {

        if (root == null || !root.isObject()) {
            throw new IllegalStateException(
                    "Invalid startup extraction JSON structure."
            );
        }

        BigDecimal funding = parseFunding(
                root.get("fundingUsdMillions")
        );

        return new StartupFeatures(
                funding,
                readText(root, "country"),
                readText(root, "industry"),
                readText(root, "problem"),
                readText(root, "solution"),
                readText(root, "targetCustomers"),
                readText(root, "businessModel")
        );
    }


    // --------------------------------------------------------
    // Funding Parsing
    // --------------------------------------------------------

    /**
     * Parses funding only when Groq returns a valid
     * non-negative numeric USD amount.
     *
     * @param node funding JSON value
     * @return funding in USD millions or null
     */
    private BigDecimal parseFunding(JsonNode node) {

        if (node == null || node.isNull()) {
            return null;
        }

        if (!node.isNumber()) {
            throw new IllegalStateException(
                    "Extracted funding must be a numeric USD value."
            );
        }

        BigDecimal funding = node.decimalValue();

        if (funding.signum() < 0) {
            throw new IllegalStateException(
                    "Extracted funding cannot be negative."
            );
        }

        return funding;
    }


    // --------------------------------------------------------
    // Text Field Parsing
    // --------------------------------------------------------

    /**
     * Reads an optional text field from the extraction JSON.
     *
     * Missing, null or blank values are converted to null.
     * Unexpected data types are rejected.
     *
     * @param root extraction JSON object
     * @param field field name
     * @return extracted text or null
     */
    private String readText(JsonNode root, String field) {

        JsonNode node = root.get(field);

        if (node == null || node.isNull()) {
            return null;
        }

        if (!node.isTextual()) {
            throw new IllegalStateException(
                    "Invalid data type for extracted field: " + field
            );
        }

        String value = node.asText().trim();

        return value.isBlank() ? null : value;
    }
}
