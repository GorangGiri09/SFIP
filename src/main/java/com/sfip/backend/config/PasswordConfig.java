
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : PasswordConfig.java
 * Package      : com.sfip.backend.config
 * File Path    : src/main/java/com/sfip/backend/config/PasswordConfig.java
 *
 * Purpose:
 * Configures password encoding for user authentication.
 *
 * Responsibilities:
 * 1. Provides a BCryptPasswordEncoder bean.
 * 2. Enables secure password hashing before saving user credentials.
 * 3. Allows Spring Security services to reuse the same PasswordEncoder.
 *
 * Security Notes:
 * - Never store raw passwords in the database.
 * - BCrypt hashes passwords; it does not encrypt them for decryption.
 * - Use the encoder's matches() method to verify login passwords.
 */

package com.sfip.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
