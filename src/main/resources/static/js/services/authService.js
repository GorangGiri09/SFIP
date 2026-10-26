
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : authService.js
 * File Path    : src/main/resources/static/js/services/authService.js
 *
 * Purpose:
 * Manages authentication-related API operations.
 *
 * Responsibilities:
 * 1. Register new users.
 * 2. Authenticate existing users.
 * 3. Store the JWT access token in the API client.
 * 4. Fetch the currently authenticated user's profile.
 * 5. Clear authentication state during logout.
 */

import {
    apiRequest,
    setAccessToken,
    clearAccessToken
} from "../api/apiClient.js";


// =========================================
// 1. VALIDATION UTILITIES
// =========================================

/**
 * Validates authentication response and stores
 * the access token returned by the backend.
 *
 * Expected AuthResponse:
 * {
 *   userId,
 *   name,
 *   email,
 *   accessToken,
 *   tokenType
 * }
 *
 * @param {object} response
 * @returns {object}
 */
function handleAuthResponse(response) {
    if (
        !response ||
        typeof response !== "object" ||
        typeof response.accessToken !== "string" ||
        !response.accessToken.trim()
    ) {
        throw new Error(
            "Authentication succeeded, but the access token was missing."
        );
    }

    if (
        response.tokenType &&
        response.tokenType.toLowerCase() !== "bearer"
    ) {
        throw new Error(
            "Unsupported authentication token type."
        );
    }

    setAccessToken(response.accessToken);

    return response;
}


// =========================================
// 2. REGISTER
// =========================================

/**
 * Supports:
 * register(name, email, password)
 * register({ name, email, password })
 *
 * @param {string|object} nameOrUserData
 * @param {string} [email]
 * @param {string} [password]
 * @returns {Promise<object>}
 */
export async function register(
    nameOrUserData,
    email,
    password
) {
    let userData;

    if (
        nameOrUserData !== null &&
        typeof nameOrUserData === "object" &&
        !Array.isArray(nameOrUserData)
    ) {
        userData = nameOrUserData;
    } else {
        userData = {
            name: nameOrUserData,
            email,
            password
        };
    }

    if (
        typeof userData.name !== "string" ||
        typeof userData.email !== "string" ||
        typeof userData.password !== "string"
    ) {
        throw new Error(
            "Invalid registration data. Please check your inputs."
        );
    }

    const normalizedName = userData.name.trim();
    const normalizedEmail = userData.email.trim();

    if (
        !normalizedName ||
        !normalizedEmail ||
        !userData.password.trim()
    ) {
        throw new Error(
            "Name, email and password are required."
        );
    }

    const response = await apiRequest("/auth/register", {
        method: "POST",
        body: {
            name: normalizedName,
            email: normalizedEmail,
            password: userData.password
        }
    });

    return handleAuthResponse(response);
}


// =========================================
// 3. LOGIN
// =========================================

/**
 * Supports:
 * login(email, password)
 * login({ email, password })
 *
 * @param {string|object} emailOrCredentials
 * @param {string} [password]
 * @returns {Promise<object>}
 */
export async function login(
    emailOrCredentials,
    password
) {
    let credentials;

    if (
        emailOrCredentials !== null &&
        typeof emailOrCredentials === "object" &&
        !Array.isArray(emailOrCredentials)
    ) {
        credentials = emailOrCredentials;
    } else {
        credentials = {
            email: emailOrCredentials,
            password
        };
    }

    if (
        typeof credentials.email !== "string" ||
        typeof credentials.password !== "string"
    ) {
        throw new Error(
            "Invalid login credentials. Please check your inputs."
        );
    }

    const normalizedEmail = credentials.email.trim();

    if (
        !normalizedEmail ||
        !credentials.password.trim()
    ) {
        throw new Error(
            "Email and password are required."
        );
    }

    const response = await apiRequest("/auth/login", {
        method: "POST",
        body: {
            email: normalizedEmail,
            password: credentials.password
        }
    });

    return handleAuthResponse(response);
}


// =========================================
// 4. CURRENT USER PROFILE
// =========================================

/**
 * Fetches the authenticated user's profile.
 *
 * Requires a valid JWT access token.
 *
 * @returns {Promise<object>}
 */
export async function getCurrentUser() {
    return apiRequest("/api/me", {
        method: "GET"
    });
}


// =========================================
// 5. LOGOUT
// =========================================

/**
 * Clears the client-side authentication token.
 *
 * This does not revoke the JWT on the server.
 */
export function logout() {
    clearAccessToken();
}
