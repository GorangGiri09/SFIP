
package com.sfip.backend.integration.ml;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import tools.jackson.databind.ObjectMapper;

/**
 * REST client responsible for communicating with the
 * SFIP Python-based Machine Learning service.
 *
 * Responsibilities:
 * 1. Check ML service availability.
 * 2. Execute startup prediction.
 * 3. Retrieve similar historical startups.
 * 4. Validate FastAPI responses.
 * 5. Log diagnostic information for HTTP failures.
 * 6. Classify failures for application-level fallback.
 */
@Component
public class MlServiceClient {

    private static final Logger logger =
            LoggerFactory.getLogger(MlServiceClient.class);

    private static final ParameterizedTypeReference<
            Map<String, Object>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    // --------------------------------------------------------
    // Constructor
    // --------------------------------------------------------

    public MlServiceClient(
            @Value("${app.ml.base-url}") String baseUrl,
            ObjectMapper objectMapper) {

        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "ML service base URL is not configured."
            );
        }

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);

        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl.trim())
                .requestFactory(requestFactory)
                .build();

        this.objectMapper = objectMapper;

        logger.info(
                "SFIP ML client initialized. Base URL={}",
                baseUrl.trim()
        );
    }

    // --------------------------------------------------------
    // Health Check
    // --------------------------------------------------------

    public Map<String, Object> checkHealth() {

        Map<String, Object> response = executeRequest(
                "ML service health check",
                "/health",
                null,
                () -> restClient.get()
                        .uri("/health")
                        .retrieve()
                        .body(RESPONSE_TYPE)
        );

        if (!"healthy".equals(response.get("status"))
                || !Boolean.TRUE.equals(response.get("model_loaded"))) {

            throw new MlServiceException(
                    FailureType.INVALID_RESPONSE,
                    "ML service health response indicates that the model is unavailable."
            );
        }

        return response;
    }

    // --------------------------------------------------------
    // Startup Prediction
    // --------------------------------------------------------

    public Map<String, Object> predict(
            Map<String, Object> features) {

        if (features == null) {
            throw new IllegalArgumentException(
                    "Prediction features cannot be null."
            );
        }

        Map<String, Object> request = new LinkedHashMap<>();

        request.put(
                "funding_usd_millions",
                features.get("funding_usd_millions")
        );

        request.put("country", features.get("country"));
        request.put("industry", features.get("industry"));

        logger.info(
                "Sending startup prediction request. Fields={}",
                request.keySet()
        );

        Map<String, Object> response = executeRequest(
                "Startup prediction",
                "/predict",
                request,
                () -> restClient.post()
                        .uri("/predict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(RESPONSE_TYPE)
        );

        validateSuccessResponse(response, "Startup prediction");

        if (!(response.get("result") instanceof Map<?, ?>)) {
            throw invalidResponse(
                    "Startup prediction response does not contain a valid result."
            );
        }

        return response;
    }

    // --------------------------------------------------------
    // Similar Startup Search
    // --------------------------------------------------------

    public Map<String, Object> findSimilarStartups(
            String text,
            String industry,
            int kEach) {

        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException(
                    "Startup description is required."
            );
        }

        if (kEach < 1 || kEach > 10) {
            throw new IllegalArgumentException(
                    "kEach must be between 1 and 10."
            );
        }

        Map<String, Object> request = new LinkedHashMap<>();

        request.put("text", text.trim());
        request.put("industry", industry);
        request.put("k_each", kEach);

        logger.info(
                "Sending similar startup request. Text length={}, industry={}, k_each={}",
                text.trim().length(),
                industry,
                kEach
        );

        Map<String, Object> response = executeRequest(
                "Similar startup search",
                "/similar-startups",
                request,
                () -> restClient.post()
                        .uri("/similar-startups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(RESPONSE_TYPE)
        );

        validateSuccessResponse(response, "Similar startup search");

        Object resultObject = response.get("result");

        if (!(resultObject instanceof Map<?, ?> result)
                || !(result.get("failed") instanceof List<?>)
                || !(result.get("succeeded") instanceof List<?>)) {

            throw invalidResponse(
                    "Similar startup response has an invalid result structure."
            );
        }

        return response;
    }

    // --------------------------------------------------------
    // Success Response Validation
    // --------------------------------------------------------

    private void validateSuccessResponse(
            Map<String, Object> response,
            String operation) {

        if (!Boolean.TRUE.equals(response.get("success"))) {
            throw invalidResponse(
                    operation + " returned an unsuccessful response."
            );
        }

        if (!"ML".equals(response.get("analysis_source"))) {
            throw invalidResponse(
                    operation + " returned an unexpected analysis source."
            );
        }
    }

    // --------------------------------------------------------
    // Common HTTP Request Execution
    // --------------------------------------------------------

    private Map<String, Object> executeRequest(
            String operation,
            String endpoint,
            Map<String, Object> requestBody,
            Supplier<Map<String, Object>> request) {

        try {

            if (requestBody != null) {
                logger.debug(
                        "{} outgoing JSON: {}",
                        operation,
                        toJson(requestBody)
                );
            }

            Map<String, Object> response = request.get();

            if (response == null) {
                throw invalidResponse(
                        operation + " returned an empty response."
                );
            }

            logger.info(
                    "{} completed successfully.",
                    operation
            );

            return response;

        } catch (RestClientResponseException exception) {

            int statusCode = exception.getStatusCode().value();

            String responseBody = exception.getResponseBodyAsString();

            FailureType failureType;

            if (statusCode == 503) {
                failureType = FailureType.SERVICE_UNAVAILABLE;
            } else if (statusCode >= 500) {
                failureType = FailureType.UPSTREAM_ERROR;
            } else {
                failureType = FailureType.HTTP_ERROR;
            }

            // Diagnostic logging: original FastAPI error response.
            logger.error(
                    """
                    ML SERVICE HTTP ERROR
                    Operation: {}
                    Endpoint: {}
                    HTTP Status: {}
                    Request JSON: {}
                    Response Body: {}
                    """,
                    operation,
                    endpoint,
                    statusCode,
                    toJson(requestBody),
                    responseBody
            );

            throw new MlServiceException(
                    failureType,
                    operation + " failed with HTTP status " + statusCode + ".",
                    statusCode,
                    exception
            );

        } catch (ResourceAccessException exception) {

            logger.error(
                    "{} failed because the ML service could not be reached or timed out.",
                    operation,
                    exception
            );

            throw new MlServiceException(
                    FailureType.CONNECTION_ERROR,
                    operation + " could not connect to the ML service.",
                    exception
            );

        } catch (RestClientException exception) {

            logger.error(
                    "{} failed because of an HTTP communication or response error.",
                    operation,
                    exception
            );

            throw new MlServiceException(
                    FailureType.RESPONSE_ERROR,
                    operation + " returned an invalid HTTP response.",
                    exception
            );
        }
    }

    // --------------------------------------------------------
    // JSON Diagnostic Helper
    // --------------------------------------------------------

    private String toJson(Object value) {

        if (value == null) {
            return "null";
        }

        try {
            return objectMapper.writeValueAsString(value);

        } catch (Exception exception) {

            logger.warn(
                    "Unable to serialize ML diagnostic request.",
                    exception
            );

            return "[JSON serialization unavailable]";
        }
    }

    // --------------------------------------------------------
    // Invalid Response Helper
    // --------------------------------------------------------

    private MlServiceException invalidResponse(String message) {

        return new MlServiceException(
                FailureType.INVALID_RESPONSE,
                message
        );
    }

    // --------------------------------------------------------
    // Failure Classification
    // --------------------------------------------------------

    public enum FailureType {

        SERVICE_UNAVAILABLE,
        CONNECTION_ERROR,
        UPSTREAM_ERROR,
        HTTP_ERROR,
        RESPONSE_ERROR,
        INVALID_RESPONSE
    }

    // --------------------------------------------------------
    // Custom ML Service Exception
    // --------------------------------------------------------

    public static class MlServiceException extends RuntimeException {

        private final FailureType failureType;
        private final Integer statusCode;

        public MlServiceException(
                FailureType failureType,
                String message) {

            super(message);

            this.failureType = failureType;
            this.statusCode = null;
        }

        public MlServiceException(
                FailureType failureType,
                String message,
                Throwable cause) {

            super(message, cause);

            this.failureType = failureType;
            this.statusCode = null;
        }

        public MlServiceException(
                FailureType failureType,
                String message,
                Integer statusCode,
                Throwable cause) {

            super(message, cause);

            this.failureType = failureType;
            this.statusCode = statusCode;
        }

        public FailureType getFailureType() {
            return failureType;
        }

        public Integer getStatusCode() {
            return statusCode;
        }
    }
}
