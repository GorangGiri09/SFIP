
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : authHandlers.js
 * File Path    : src/main/resources/static/js/modules/authHandlers.js
 *
 * Purpose:
 * Handles authentication form interactions.
 *
 * Responsibilities:
 * 1. Handle login submission.
 * 2. Handle registration submission.
 * 3. Validate user input.
 * 4. Display authentication messages.
 * 5. Manage button loading states.
 * 6. Toggle password visibility.
 * 7. Redirect after successful authentication.
 */

import {
    login,
    register,
    getCurrentUser,
    logout
} from "../services/authService.js";


// =========================================
// 1. DOM ELEMENTS
// =========================================

const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");

const loginButton = document.getElementById("login-submit");
const registerButton = document.getElementById("register-submit");

const loginMessage = document.getElementById("login-message");
const registerMessage = document.getElementById("register-message");


// =========================================
// 2. CONFIGURATION
// =========================================

const MAX_NAME_LENGTH = 100;
const MAX_EMAIL_LENGTH = 255;
const MIN_PASSWORD_LENGTH = 8;
const MAX_PASSWORD_BYTES = 72;

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;


// =========================================
// 3. SUBMISSION STATE
// =========================================

let loginInProgress = false;
let registrationInProgress = false;
let handlersInitialized = false;


// =========================================
// 4. INPUT VALUE HELPER
// =========================================

/**
 * Returns the value of an input element.
 *
 * @param {string} inputId
 * @returns {string}
 */
function getInputValue(inputId) {
    const input = document.getElementById(inputId);

    if (!input) {
        throw new Error(
            `Input element not found: ${inputId}`
        );
    }

    if (typeof input.value !== "string") {
        throw new Error(
            `Invalid input element or value: ${inputId}`
        );
    }

    return input.value;
}


// =========================================
// 5. INITIALIZE AUTH HANDLERS
// =========================================

export function initializeAuthHandlers() {
    if (handlersInitialized) {
        return;
    }

    if (
        !loginForm ||
        !registerForm ||
        !loginButton ||
        !registerButton ||
        !loginMessage ||
        !registerMessage
    ) {
        console.error(
            "Authentication initialization failed: " +
            "Required DOM elements are missing."
        );

        return;
    }

    loginForm.addEventListener(
        "submit",
        handleLogin
    );

    registerForm.addEventListener(
        "submit",
        handleRegister
    );

    setupPasswordToggle(
        "login-password",
        "login-password-toggle"
    );

    setupPasswordToggle(
        "register-password",
        "register-password-toggle"
    );

    handlersInitialized = true;
}


// =========================================
// 6. PASSWORD SHOW / HIDE
// =========================================

function setupPasswordToggle(inputId, buttonId) {
    const passwordInput =
        document.getElementById(inputId);

    const toggleButton =
        document.getElementById(buttonId);

    if (!passwordInput || !toggleButton) {
        console.warn(
            `Password toggle elements unavailable: ${inputId}`
        );

        return;
    }

    // Prevent accidental form submission.
    toggleButton.type = "button";

    toggleButton.addEventListener("click", () => {
        const isHidden =
            passwordInput.type === "password";

        passwordInput.type = isHidden
            ? "text"
            : "password";

        toggleButton.setAttribute(
            "aria-label",
            isHidden ? "Hide password" : "Show password"
        );

        toggleButton.setAttribute(
            "aria-pressed",
            String(isHidden)
        );

        passwordInput.focus({
            preventScroll: true
        });
    });
}


// =========================================
// 7. LOGIN HANDLER
// =========================================

async function handleLogin(event) {
    event.preventDefault();

    if (loginInProgress) {
        return;
    }

    clearMessage(loginMessage);

    let authenticationSucceeded = false;

    try {
        const email =
            getInputValue("login-email").trim();

        // Never trim passwords.
        const password =
            getInputValue("login-password");

        if (!email || !password) {
            showMessage(
                loginMessage,
                "Please enter your email and password.",
                "error"
            );

            return;
        }

        if (
            email.length > MAX_EMAIL_LENGTH ||
            !EMAIL_PATTERN.test(email)
        ) {
            showMessage(
                loginMessage,
                "Please enter a valid email address.",
                "error"
            );

            return;
        }

        loginInProgress = true;

        setLoading(
            loginButton,
            true,
            "Logging in..."
        );

        // Authenticate user and store JWT.
        await login(email, password);

        authenticationSucceeded = true;

        // Verify authentication using backend profile.
        await getCurrentUser();

        showMessage(
            loginMessage,
            "Login successful. Redirecting...",
            "success"
        );

        window.location.assign("/dashboard.html");

    } catch (error) {
        console.error("Login failed:", error);

        // Clear authentication only if login succeeded
        // but subsequent profile verification failed.
        if (authenticationSucceeded) {
            try {
                logout();
            } catch (cleanupError) {
                console.error(
                    "Authentication cleanup failed:",
                    cleanupError
                );
            }
        }

        showMessage(
            loginMessage,
            getErrorMessage(error, "login"),
            "error"
        );

    } finally {
        loginInProgress = false;

        setLoading(
            loginButton,
            false,
            "Sign in"
        );
    }
}


// =========================================
// 8. REGISTRATION HANDLER
// =========================================

async function handleRegister(event) {
    event.preventDefault();

    if (registrationInProgress) {
        return;
    }

    clearMessage(registerMessage);

    let authenticationSucceeded = false;

    try {
        const name =
            getInputValue("register-name").trim();

        const email =
            getInputValue("register-email").trim();

        // Never trim passwords.
        const password =
            getInputValue("register-password");

        // Required fields.
        if (!name || !email || !password) {
            showMessage(
                registerMessage,
                "Please fill in all required fields.",
                "error"
            );

            return;
        }

        // Name validation.
        if (name.length > MAX_NAME_LENGTH) {
            showMessage(
                registerMessage,
                "Name cannot exceed 100 characters.",
                "error"
            );

            return;
        }

        // Email validation.
        if (
            email.length > MAX_EMAIL_LENGTH ||
            !EMAIL_PATTERN.test(email)
        ) {
            showMessage(
                registerMessage,
                "Please enter a valid email address.",
                "error"
            );

            return;
        }

        // Password validation.
        if (password.length < MIN_PASSWORD_LENGTH) {
            showMessage(
                registerMessage,
                "Password must be at least 8 characters long.",
                "error"
            );

            return;
        }

        // Match backend BCrypt UTF-8 byte limit.
        const passwordBytes =
            new TextEncoder().encode(password).length;

        if (passwordBytes > MAX_PASSWORD_BYTES) {
            showMessage(
                registerMessage,
                "Password must not exceed 72 UTF-8 bytes.",
                "error"
            );

            return;
        }

        registrationInProgress = true;

        setLoading(
            registerButton,
            true,
            "Creating account..."
        );

        // Register user and store JWT.
        await register(name, email, password);

        authenticationSucceeded = true;

        // Verify authentication using backend profile.
        await getCurrentUser();

        showMessage(
            registerMessage,
            "Account created successfully. Redirecting...",
            "success"
        );

        window.location.assign("/dashboard.html");

    } catch (error) {
        console.error(
            "Registration failed:",
            error
        );

        // Registration may have succeeded on the server.
        // Clear client authentication if profile verification
        // fails after a successful auth response.
        if (authenticationSucceeded) {
            try {
                logout();
            } catch (cleanupError) {
                console.error(
                    "Authentication cleanup failed:",
                    cleanupError
                );
            }
        }

        showMessage(
            registerMessage,
            getErrorMessage(error, "register"),
            "error"
        );

    } finally {
        registrationInProgress = false;

        setLoading(
            registerButton,
            false,
            "Create account"
        );
    }
}


// =========================================
// 9. MESSAGE HANDLING
// =========================================

function showMessage(element, message, type) {
    if (!element) {
        return;
    }

    element.textContent = message;

    element.classList.remove(
        "error",
        "success"
    );

    element.classList.add(type);

    element.hidden = false;

    element.setAttribute(
        "role",
        type === "error" ? "alert" : "status"
    );
}


function clearMessage(element) {
    if (!element) {
        return;
    }

    element.textContent = "";

    element.classList.remove(
        "error",
        "success"
    );

    element.hidden = true;
}


// =========================================
// 10. BUTTON LOADING STATE
// =========================================

function setLoading(button, loading, text) {
    if (!button) {
        return;
    }

    button.disabled = loading;

    button.setAttribute(
        "aria-busy",
        String(loading)
    );

    button.replaceChildren(
        document.createTextNode(text)
    );

    if (!loading) {
        const arrow = document.createElement("span");

        arrow.setAttribute(
            "aria-hidden",
            "true"
        );

        arrow.textContent = " →";

        button.appendChild(arrow);
    }
}


// =========================================
// 11. ERROR MESSAGE HANDLING
// =========================================

function getErrorMessage(error, operation) {
    const status =
        error?.response?.status ??
        error?.status;

    const errorCode = error?.code;

    // Network errors.
    if (
        errorCode === "NETWORK_ERROR" ||
        errorCode === "REQUEST_TIMEOUT" ||
        status === 0
    ) {
        return errorCode === "REQUEST_TIMEOUT"
            ? "The request timed out. Please try again."
            : "Unable to connect to the server. Please check your connection.";
    }

    // Invalid input.
    if (status === 400) {
        return (
            error?.data?.message ??
            error?.response?.data?.message ??
            "Invalid input. Please check your details."
        );
    }

    // Authentication failure.
    if (status === 401) {
        return operation === "login"
            ? "Invalid email or password."
            : "Authentication failed. Please try again.";
    }

    // Forbidden.
    if (status === 403) {
        return "Access denied. Please check your authentication.";
    }

    // Duplicate account.
    if (status === 409) {
        return "An account with this email may already exist.";
    }

    // Server errors.
    if (status >= 500) {
        return "Server error. Please try again later.";
    }

    return (
        error?.data?.message ??
        error?.response?.data?.message ??
        error?.message ??
        "Something went wrong. Please try again."
    );
}
