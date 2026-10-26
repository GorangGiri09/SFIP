
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : JwtService.java
 * Package      : com.sfip.backend.service
 * File Path    : src/main/java/com/sfip/backend/service/JwtService.java
 *
 * Purpose:
 * Handles JWT access token generation and validation.
 *
 * Responsibilities:
 * 1. Generate signed JWT access tokens.
 * 2. Extract user email from JWT claims.
 * 3. Validate token signature and expiration.
 * 4. Provide token expiry configuration.
 *
 * Security Notes:
 * - JWT signing secret must be stored securely.
 * - Never include passwords or password hashes in JWT claims.
 * - This service does not authenticate HTTP requests by itself.
 */

package com.sfip.backend.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:900000}") long expirationMs) {

        // Secret is expected to be Base64 encoded.
        byte[] keyBytes = Decoders.BASE64.decode(secret);

        // HS256 requires a sufficiently strong key.
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;

        if (expirationMs <= 0) {
            throw new IllegalArgumentException(
                    "JWT expiration must be positive"
            );
        }
    }

    // Generate JWT access token for authenticated user.
    public String generateToken(UserDetails userDetails) {

        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    // Extract email (JWT subject).
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Validate token signature and expiration.
    public boolean isTokenValid(
            String token,
            UserDetails userDetails) {

        Claims claims = extractAllClaims(token);

        return claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date());
    }

    // Parse and verify signed JWT.
    private Claims extractAllClaims(String token) {

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
