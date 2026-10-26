
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : GlobalExceptionHandler.java
 * Package      : com.sfip.backend.exception
 * File Path    : src/main/java/com/sfip/backend/exception/GlobalExceptionHandler.java
 *
 * Purpose:
 * Provides centralized exception handling for REST APIs.
 *
 * Responsibilities:
 * 1. Handle request validation errors.
 * 2. Handle duplicate registration errors.
 * 3. Handle authentication failures.
 * 4. Return consistent API error responses.
 * 5. Avoid exposing internal exception details.
 *
 * Security Notes:
 * - Never expose stack traces or database error messages.
 * - Do not reveal account existence during login.
 */

package com.sfip.backend.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle invalid request DTO fields.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getDefaultMessage())
                .filter(msg -> msg != null && !msg.isBlank())
                .findFirst()
                .orElse("Invalid request data");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "success", false,
                        "status", 400,
                        "message", message
                ));
    }

    // Handle duplicate email or conflicting database constraints.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "success", false,
                        "status", 409,
                        "message",
                        "The requested data conflicts with an existing record"
                ));
    }

    // Handle incorrect login credentials.
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "success", false,
                        "status", 401,
                        "message", "Invalid email or password"
                ));
    }

    // Handle other Spring Security authentication failures.
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthentication(
            AuthenticationException ex) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(
                        "success", false,
                        "status", 401,
                        "message", "Authentication failed"
                ));
    }

    // Handle application-level bad requests.
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(
            IllegalArgumentException ex) {

        String message = ex.getMessage();

        if (message == null || message.isBlank()) {
            message = "Invalid request";
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "success", false,
                        "status", 400,
                        "message", message
                ));
    }

    // Fallback for unexpected errors.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(
            Exception ex) {

        // Log actual exception and stack trace on the server.
        // Do not expose internal details to the client.
        log.error("Unexpected server error", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                        "success", false,
                        "status", 500,
                        "message", "An unexpected error occurred"
                ));
    }
}
