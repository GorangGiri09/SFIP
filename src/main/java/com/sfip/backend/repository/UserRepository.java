
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : UserRepository.java
 * Package      : com.sfip.backend.repository
 * File Path    : src/main/java/com/sfip/backend/repository/UserRepository.java
 *
 * Purpose:
 * Provides database access operations for User entities.
 *
 * Responsibilities:
 * 1. Performs basic CRUD operations on users.
 * 2. Finds users by email for authentication.
 * 3. Checks whether an email is already registered.
 *
 * Database Table: users
 */

package com.sfip.backend.repository;

import com.sfip.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Finds a user using their email address.
    Optional<User> findByEmail(String email);

    // Checks whether an email is already registered.
    boolean existsByEmail(String email);
}
