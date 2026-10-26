
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : UserDetailsServiceImpl.java
 * Package      : com.sfip.backend.service
 * File Path    : src/main/java/com/sfip/backend/service/UserDetailsServiceImpl.java
 *
 * Purpose:
 * Loads user authentication details from the database.
 *
 * Responsibilities:
 * 1. Finds a registered user using their email address.
 * 2. Provides the stored password hash to Spring Security.
 * 3. Assigns the ROLE_USER authority.
 * 4. Throws UsernameNotFoundException if the user does not exist.
 *
 * Security Notes:
 * - Never return the User entity directly from authentication APIs.
 * - Password verification is handled using PasswordEncoder.
 * - This class does not generate JWT tokens.
 */

package com.sfip.backend.service;

import java.util.Locale;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.sfip.backend.entity.User;
import com.sfip.backend.repository.UserRepository;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Invalid email or password"
                        )
                );

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();
    }
}
