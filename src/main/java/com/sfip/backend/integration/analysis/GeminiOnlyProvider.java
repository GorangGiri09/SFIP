
package com.sfip.backend.integration.analysis;

import com.sfip.backend.integration.ai.AiService;
import org.springframework.stereotype.Component;

/**
 * File Name: GeminiOnlyProvider.java
 * Project: Startup Failure Intelligence Platform (SFIP)
 *
 * Description:
 * Implements the AnalysisProvider contract using
 * Gemini AI for startup idea analysis.
 *
 * Responsibilities:
 * 1. Accept the user's startup idea.
 * 2. Prepare the startup analysis prompt.
 * 3. Delegate AI communication to AiService.
 * 4. Return the analysis report in the standardized
 *    AnalysisResult format.
 *
 * Notes:
 * - This is the temporary analysis provider.
 * - The ML model is not integrated yet.
 * - Gemini API communication is handled by AiService.
 * - Future ML integration will use a separate provider
 *   implementation without changing this contract.
 *
 * Author: SFIP Development Team
 */
@Component
public class GeminiOnlyProvider implements AnalysisProvider {

    private final AiService aiService;

    public GeminiOnlyProvider(AiService aiService) {
        this.aiService = aiService;
    }

    /**
     * Analyzes a startup idea using Gemini AI.
     *
     * @param startupIdea the user's startup idea
     * @return standardized analysis result
     */
    @Override
    public AnalysisResult analyze(String startupIdea) {

        if (startupIdea == null || startupIdea.isBlank()) {
            throw new IllegalArgumentException(
                "Startup idea cannot be empty."
            );
        }

        String prompt = buildAnalysisPrompt(startupIdea.trim());

        String report = aiService.generateReply(prompt);

        return new AnalysisResult(report);
    }

    /**
     * Builds the prompt used for startup analysis.
     *
     * @param startupIdea validated startup idea
     * @return analysis prompt
     */
    private String buildAnalysisPrompt(String startupIdea) {

        return """
            You are a startup failure analysis assistant for
            the Startup Failure Intelligence Platform (SFIP).

            Your primary objective is to analyze the user's
            startup idea by learning from similar failed startups
            and recommending ways to reduce similar risks.

            STARTUP IDEA:
            %s

            REPORT STRUCTURE:

            1. Startup Idea Summary
               Explain the idea in simple language.

            2. Problem Statement
               Identify the problem being addressed.

            3. Proposed Solution
               Explain how the startup solves the problem.

            4. Target Customers
               Identify potential customer segments.

            5. Similar Failed Startups
               Identify startups that are relevantly similar
               to the user's idea and have shut down or failed.

               For each startup, provide:
               - Startup name
               - What the startup did
               - Its target customers
               - Documented reason(s) for failure or shutdown
               - How it is similar to the user's idea
               - Key lesson the user can learn
               - Reliable source name and source URL, if known

               Match startups based on problem, solution,
               target audience, industry and business model.

               Prefer genuinely comparable cases over a
               longer list of weakly related startups.

               Include only relevant cases supported by
               reliable information. If no suitable cases
               can be identified confidently, say so.

               Do not classify acquisitions, mergers or
               successful exits as failures without evidence
               of an actual shutdown or failure.

            6. Failure Pattern Analysis
               Identify common failure patterns among the
               relevant cases, such as:
               - Lack of product-market fit
               - Customer acquisition difficulties
               - Cash flow or funding problems
               - Weak monetization
               - Operational challenges
               - Competition

               Include only patterns supported by the cases.
               Do not assume every factor applies.

            7. Market Opportunity
               Discuss potential market opportunities.
               Clearly distinguish verified facts from
               estimates and assumptions.

            8. Competitor Analysis
               Discuss relevant competitor categories.
               Do not invent competitor facts.

            9. Revenue Model
               Suggest suitable revenue streams and
               explain the reasoning.

            10. Potential Failure Factors
                Identify the major risks for this idea.
                Connect each risk to relevant failed startups
                where evidence supports the connection.
                Clearly label other risks as hypotheses.

            11. Strengths and Weaknesses
                Present key strengths and limitations.

            12. Failure Prevention Strategy
                Recommend specific actions the founder
                should take to reduce identified risks.

                For each major recommendation, provide:
                - Risk being addressed
                - Recommended action
                - Practical implementation steps
                - A measurable validation metric
                - Suggested priority: High, Medium or Low

                Prefer low-cost validation before major
                investments or scaling.

            13. Action Plan
                Give a prioritized, practical roadmap:
                - Immediate next steps
                - Short-term validation
                - Conditions to meet before scaling

                Treat timelines and metric targets as
                suggestions, not guaranteed benchmarks.

            14. Overall Assessment
                Give a balanced qualitative assessment
                explaining the opportunity, key risks,
                evidence limitations and next steps.

            IMPORTANT INSTRUCTIONS:

            - Base the analysis on the provided idea.
            - Never invent startup names, shutdown events,
              failure causes, sources or source URLs.
            - Never fabricate similarity percentages,
              market statistics or financial figures.
            - Clearly distinguish documented facts,
              assumptions, hypotheses and recommendations.
            - If a failure reason is disputed or uncertain,
              disclose the uncertainty.
            - If reliable evidence is unavailable, explicitly
              state that the case could not be verified.
            - Do not treat similarity to failed startups as
              proof that the user's startup will fail.
            - Do not claim success or failure is certain.
            - Keep recommendations specific and actionable.
            - Use clear headings and readable formatting.
            - Do not provide generic advice without explaining
              its relevance to the user's idea.

            """.formatted(startupIdea);
    }
}
