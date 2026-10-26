
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : User.java
 * Package      : com.sfip.backend.entity
 * File Path    : src/main/java/com/sfip/backend/entity/User.java
 *
 * Purpose:
 * Represents a registered user of the SFIP application.
 *
 * Responsibilities:
 * 1. Stores user identity and account details.
 * 2. Maintains a unique email for each user.
 * 3. Stores the encoded password for authentication.
 * 4. Maintains the account creation timestamp.
 *
 * Database Table: users
 *
 * Note:
 * Password hashing will be handled in the authentication
 * service using BCrypt before saving a user.
 */

package com.sfip.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_users_email",
            columnNames = "email"
        )
    }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    // Stores the encoded password, never plain text.
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @CreationTimestamp
    @Column(
        name = "created_at",
        nullable = false,
        updatable = false
    )
    private LocalDateTime createdAt;

    // Default constructor required by JPA.
    public User() {
    }

    // Constructor for creating a user.
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
