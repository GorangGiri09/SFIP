
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : JwtAuthenticationFilter.java
 * Package      : com.sfip.backend.service
 * File Path    : src/main/java/com/sfip/backend/service/JwtAuthenticationFilter.java
 *
 * Purpose:
 * Validates JWT access tokens for incoming HTTP requests.
 *
 * Responsibilities:
 * 1. Read the Authorization Bearer token.
 * 2. Extract the user's email from the token.
 * 3. Load the corresponding user details.
 * 4. Validate the token.
 * 5. Set the authenticated user in SecurityContext.
 *
 * Security Notes:
 * - Requests without a Bearer token continue through the filter chain.
 * - Invalid tokens are not authenticated.
 * - Authorization rules are configured in SecurityConfig.
 * - Never log raw JWT tokens or secrets.
 */

package com.sfip.backend.service;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserDetailsService userDetailsService) {

        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // No Bearer token: continue normally.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();

        // Reject an empty Bearer token.
        if (token.isEmpty()) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid access token"
            );
            return;
        }

        try {
            String email = jwtService.extractEmail(token);

            // Avoid replacing an existing authentication.
            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                UserDetails userDetails =
                        userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request)
                    );

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                } else {
                    response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "Invalid access token"
                    );
                    return;
                }
            }

        } catch (JwtException | IllegalArgumentException |
                 UsernameNotFoundException ex) {

            SecurityContextHolder.clearContext();

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired access token"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }
}
