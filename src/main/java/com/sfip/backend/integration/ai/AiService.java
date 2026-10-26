
package com.sfip.backend.integration.ai;

import tools.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Service responsible for communicating with the Groq API.
 *
 * Responsibilities:
 * 1. Validate prompt and API configuration.
 * 2. Construct the Groq chat completion request.
 * 3. Execute the HTTP request using RestClient.
 * 4. Parse and validate the generated response.
 * 5. Log API failures without exposing sensitive credentials.
 *
 * AI communication only.
 * Session management, report persistence, and analysis
 * orchestration are handled separately.
 */
@Service
public class AiService {

    private static final Logger log =
            LoggerFactory.getLogger(AiService.class);

    private static final String GROQ_API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    /**
     * Initializes the Groq AI service.
     *
     * @param apiKey Groq API key
     * @param model Groq model identifier
     */
    public AiService(
            @Value("${groq.api.key:}") String apiKey,
            @Value("${groq.api.model:openai/gpt-oss-20b}")
            String model) {

        this.apiKey = apiKey;
        this.model = model;

        /*
         * Create HTTP client without an explicitly
         * configured connection timeout.
         */
        java.net.http.HttpClient httpClient =
                java.net.http.HttpClient.newBuilder()
                        .build();

        /*
         * Do not configure a read timeout here.
         */
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Sends a prompt to Groq and returns generated text.
     *
     * Constructs a chat completion request using the
     * OpenAI-compatible Groq API format.
     *
     * @param prompt prompt to send
     * @return generated AI response
     * @throws IllegalArgumentException if prompt is empty
     * @throws IllegalStateException if configuration is invalid
     *         or the AI request fails
     */
    public String generateReply(String prompt) {

        validateConfiguration(prompt);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "max_completion_tokens", 8192
        );

        long startTime = System.nanoTime();

        try {

            JsonNode response = restClient.post()
                    .uri(GROQ_API_URL)
                    .header(
                            "Authorization",
                            "Bearer " + apiKey
                    )
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);

            return extractGeneratedText(response);

        } catch (RestClientException exception) {

            long elapsedMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            log.error(
                    "Groq API request failed. Model={}, elapsedMs={}, exceptionType={}",
                    model,
                    elapsedMs,
                    exception.getClass().getSimpleName(),
                    exception
            );

            throw new IllegalStateException(
                    "Unable to get a response from the AI service.",
                    exception
            );
        }
    }

    /**
     * Validates the input prompt and AI configuration.
     *
     * @param prompt prompt to validate
     * @throws IllegalArgumentException if prompt is empty
     * @throws IllegalStateException if the request is interrupted
     *         or AI configuration is missing
     */
    private void validateConfiguration(String prompt) {

        if (Thread.currentThread().isInterrupted()) {
            throw new IllegalStateException(
                    "AI request was interrupted."
            );
        }

        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException(
                    "AI prompt cannot be empty."
            );
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "Groq API key is not configured."
            );
        }

        if (model == null || model.isBlank()) {
            throw new IllegalStateException(
                    "Groq model is not configured."
            );
        }
    }

    /**
     * Extracts generated text from the Groq response.
     *
     * Reads the content from the first choice's
     * message content field.
     *
     * @param response Groq API response
     * @return generated text
     * @throws IllegalStateException if the response
     *         is null, invalid, or contains no text
     */
    private String extractGeneratedText(JsonNode response) {

        if (response == null) {
            throw new IllegalStateException(
                    "Groq returned an empty response."
            );
        }

        JsonNode choices = response.path("choices");

        if (!choices.isArray() || choices.isEmpty()) {
            throw new IllegalStateException(
                    "Groq did not return a valid response."
            );
        }

        JsonNode content =
                choices.get(0)
                        .path("message")
                        .path("content");

        if (!content.isTextual()) {
            throw new IllegalStateException(
                    "Groq response content is invalid."
            );
        }

        String generatedText = content.asText().trim();

        if (generatedText.isBlank()) {
            throw new IllegalStateException(
                    "Groq returned no text content."
            );
        }

        return generatedText;
    }
}
