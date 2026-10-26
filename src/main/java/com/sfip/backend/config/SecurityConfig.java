
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : SecurityConfig.java
 * Package      : com.sfip.backend.config
 * File Path    : src/main/java/com/sfip/backend/config/SecurityConfig.java
 *
 * Purpose:
 * Configures Spring Security for the SFIP backend.
 *
 * Responsibilities:
 * 1. Configure public authentication endpoints.
 * 2. Allow access to public frontend resources.
 * 3. Protect remaining API endpoints.
 * 4. Register JWT authentication in the security filter chain.
 * 5. Disable session-based authentication.
 * 6. Configure AuthenticationManager for login.
 * 7. Prevent duplicate servlet registration of JWT filter.
 *
 * Security Notes:
 * - JWT is used for stateless authentication.
 * - CSRF is disabled for this stateless Bearer-token API.
 * - Password verification is handled by Spring Security.
 * - CORS should be configured separately if required.
 * - Public dashboard HTML does not make private APIs public.
 */

package com.sfip.backend.config;

import com.sfip.backend.service.JwtAuthenticationFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            // Stateless Bearer-token API.
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Public authentication endpoints.
                .requestMatchers(
                    "/auth/register",
                    "/auth/login"
                ).permitAll()

                // Public frontend pages and static resources.
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/dashboard.html",
                    "/favicon.ico",
                    "/css/**",
                    "/js/**",
                    "/assets/**"
                ).permitAll()

                // Remaining endpoints require authentication.
                .anyRequest().authenticated()
            )

            // Validate JWT before username/password authentication.
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration)
            throws Exception {

        return configuration.getAuthenticationManager();
    }

    /*
     * The JWT filter is already added to Spring Security's
     * filter chain. Disable automatic servlet registration
     * to avoid duplicate filter execution.
     */
    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter>
            jwtFilterServletRegistration() {

        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>();

        registration.setFilter(jwtAuthenticationFilter);
        registration.setEnabled(false);

        return registration;
    }
}
