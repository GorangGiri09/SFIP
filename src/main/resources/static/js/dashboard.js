/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : dashboard.js
 * File Path    : static/js/dashboard.js
 * Project      : Startup Failure Intelligence Platform (SFIP)
 *
 * Description:
 * Initializes the SFIP dashboard and manages dashboard-level
 * interactions and authenticated user experience.
 *
 * Responsibilities:
 * 1. Verify authenticated user session.
 * 2. Load and display current user profile.
 * 3. Handle logout functionality.
 * 4. Manage startup idea character counter.
 * 5. Connect analysis submission with dashboardHandlers.js.
 * 6. Load user's analysis history.
 * 7. Initialize dashboard sidebar navigation.
 *
 * Dependencies:
 * - services/authService.js
 * - api/apiClient.js
 * - modules/dashboardHandlers.js
 */

// =========================================
// 1. IMPORTS
// =========================================

import {
    getCurrentUser,
    logout
} from "./services/authService.js";

import {
    getAccessToken
} from "./api/apiClient.js";

import {
    handleAnalysisSubmit,
    loadAnalysisHistory
} from "./modules/dashboardHandlers.js";


// =========================================
// 2. CONFIGURATION
// =========================================

const MIN_IDEA_LENGTH = 20;
const MAX_IDEA_LENGTH = 10000;


// =========================================
// 3. DOM UTILITIES
// =========================================

/**
 * Returns an element using its ID.
 *
 * @param {string} id
 * @returns {HTMLElement|null}
 */
function getElement(id) {
    return document.getElementById(id);
}


// =========================================
// 4. DASHBOARD MESSAGES
// =========================================

/**
 * Displays a dashboard message.
 *
 * @param {string} message
 * @param {string} type
 */
function showMessage(message, type = "info") {
    const messageElement = getElement("analysis-message");

    if (!messageElement) {
        return;
    }

    messageElement.textContent = message;
    messageElement.dataset.type = type;
    messageElement.hidden = false;
}


// =========================================
// 5. AUTHENTICATION
// =========================================

/**
 * Redirects unauthenticated users to login.
 */
function redirectToLogin() {
    window.location.replace("/login.html");
}

/**
 * Checks whether an access token exists.
 *
 * This is only a local token-presence check.
 * Actual authentication is verified through /api/me.
 *
 * @returns {boolean}
 */
function checkAuthentication() {
    const token = getAccessToken();

    if (!token) {
        redirectToLogin();
        return false;
    }

    return true;
}


// =========================================
// 6. USER PROFILE
// =========================================

/**
 * Generates initials from a user's name.
 *
 * @param {string} name
 * @returns {string}
 */
function getInitials(name) {
    if (typeof name !== "string" || !name.trim()) {
        return "U";
    }

    return name
        .trim()
        .split(/\s+/)
        .slice(0, 2)
        .map(part => part.charAt(0).toUpperCase())
        .join("");
}

/**
 * Updates dashboard profile information.
 *
 * @param {object} user
 */
function renderUserProfile(user) {
    if (!user || typeof user !== "object") {
        return;
    }

    const name =
        user.name ??
        user.fullName ??
        user.username ??
        "User";

    const email = user.email ?? "";

    const nameElement = getElement("user-name");
    const emailElement = getElement("user-email");
    const avatarElement = getElement("user-avatar");

    if (nameElement) {
        nameElement.textContent = name;
    }

    if (emailElement) {
        emailElement.textContent = email;
    }

    if (avatarElement) {
        avatarElement.textContent = getInitials(name);
    }
}

/**
 * Loads and verifies the authenticated user's profile.
 *
 * @returns {Promise<boolean>}
 */
async function loadUserProfile() {
    try {
        const response = await getCurrentUser();

        const data = response?.data ?? response;
        const user = data?.user ?? data;

        if (
            !user ||
            typeof user !== "object" ||
            Array.isArray(user)
        ) {
            throw new Error(
                "Invalid user profile response."
            );
        }

        renderUserProfile(user);

        return true;

    } catch (error) {
        console.error(
            "Profile loading error:",
            error
        );

        // Invalid or expired authentication.
        if (
            error?.status === 401 ||
            error?.status === 403
        ) {
            logout();
            redirectToLogin();
            return false;
        }

        showMessage(
            "Unable to verify your profile. Please try again.",
            "error"
        );

        return false;
    }
}


// =========================================
// 7. LOGOUT
// =========================================

/**
 * Initializes logout functionality.
 */
function initializeLogout() {
    const logoutButton = getElement("logout-btn");

    if (!logoutButton) {
        return;
    }

    logoutButton.addEventListener("click", () => {
        logout();
        window.location.replace("/login.html");
    });
}


// =========================================
// 8. CHARACTER COUNTER
// =========================================

/**
 * Initializes the startup idea character counter.
 */
function initializeCharacterCounter() {
    const ideaInput = getElement("startup-idea");

    const counterElement = getElement(
        "idea-character-count"
    );

    if (!ideaInput || !counterElement) {
        console.warn(
            "Character counter elements were not found."
        );
        return;
    }

    /**
     * Updates the character counter.
     */
    const updateCounter = () => {
        const length = ideaInput.value.length;

        counterElement.textContent =
            `${length} / ${MAX_IDEA_LENGTH}`;

        counterElement.dataset.valid = String(
            length >= MIN_IDEA_LENGTH &&
            length <= MAX_IDEA_LENGTH
        );
    };

    ideaInput.addEventListener(
        "input",
        updateCounter
    );

    // Set initial counter value.
    updateCounter();
}


// =========================================
// 9. ANALYSIS FORM
// =========================================

/**
 * Initializes the startup analysis form.
 *
 * Validation and submission are handled by
 * dashboardHandlers.js.
 */
function initializeAnalysisForm() {
    const analysisForm = getElement("analysis-form");
    const ideaInput = getElement("startup-idea");

    if (!analysisForm || !ideaInput) {
        console.warn(
            "Analysis form elements were not found."
        );
        return;
    }

    // Use a single submit listener.
    // Avoid duplicate validation and duplicate requests.
    analysisForm.addEventListener(
        "submit",
        handleAnalysisSubmit
    );
}


// =========================================
// 10. SIDEBAR NAVIGATION
// =========================================

/**
 * Initializes dashboard sidebar navigation.
 *
 * Handles:
 * 1. Mobile sidebar toggle.
 * 2. Active navigation link.
 * 3. Hash-based section navigation.
 * 4. aria-current accessibility state.
 */
function initializeSidebar() {
    const sidebarToggle = getElement("sidebar-toggle");
    const sidebar = getElement("sidebar");

    // Mobile sidebar toggle.
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener(
            "click",
            () => {
                const isActive =
                    sidebar.classList.toggle("active");

                sidebarToggle.setAttribute(
                    "aria-expanded",
                    String(isActive)
                );
            }
        );
    }

    const navLinks = document.querySelectorAll(
        ".sidebar-nav .nav-link"
    );

    if (!navLinks.length) {
        return;
    }

    /**
     * Updates the active navigation link.
     */
    function updateActiveLink() {
        const currentHash = window.location.hash;

        navLinks.forEach((link) => {
            const href = link.getAttribute("href");

            let isActive = false;

            if (currentHash) {
                isActive = href === currentHash;
            } else {
                isActive =
                    href === "/dashboard.html" ||
                    href === "dashboard.html";
            }

            link.classList.toggle(
                "active",
                isActive
            );

            if (isActive) {
                link.setAttribute(
                    "aria-current",
                    "page"
                );
            } else {
                link.removeAttribute("aria-current");
            }
        });
    }

    navLinks.forEach((link) => {
        link.addEventListener("click", () => {
            const href = link.getAttribute("href");

            if (!href) {
                return;
            }

            // Navigate to the requested section.
            if (href.startsWith("#")) {
                const target = getElement(
                    href.substring(1)
                );

                if (target) {
                    target.scrollIntoView({
                        behavior: "smooth",
                        block: "start"
                    });
                }
            }

            // Close mobile sidebar after navigation.
            if (sidebar) {
                sidebar.classList.remove("active");
            }

            if (sidebarToggle) {
                sidebarToggle.setAttribute(
                    "aria-expanded",
                    "false"
                );
            }

            updateActiveLink();
        });
    });

    // Handle browser hash navigation.
    window.addEventListener(
        "hashchange",
        updateActiveLink
    );

    // Set initial active link.
    updateActiveLink();
}


// =========================================
// 11. DASHBOARD INITIALIZATION
// =========================================

/**
 * Initializes dashboard components.
 */
async function initializeDashboard() {
    // STEP 1: Check local authentication.
    if (!checkAuthentication()) {
        return;
    }

    // STEP 2: Initialize dashboard interactions.
    initializeLogout();
    initializeCharacterCounter();
    initializeAnalysisForm();
    initializeSidebar();

    // STEP 3: Verify authentication and load profile.
    const profileLoaded = await loadUserProfile();

    if (!profileLoaded) {
        return;
    }

    // STEP 4: Load saved analysis history.
    // History loading handles its own errors.
    await loadAnalysisHistory();
}


// =========================================
// 12. DOM READY
// =========================================

/**
 * Initializes the dashboard after the DOM is ready.
 */
if (document.readyState === "loading") {
    document.addEventListener(
        "DOMContentLoaded",
        initializeDashboard,
        { once: true }
    );
} else {
    initializeDashboard();
}