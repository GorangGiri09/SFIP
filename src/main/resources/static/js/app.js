
/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : app.js
 * File Path    : src/main/resources/static/js/app.js
 * Package      : N/A (Frontend JavaScript ES Module)
 *
 * Purpose:
 * Main entry point of the SFIP frontend application.
 *
 * Responsibilities:
 * 1. Initialize the authentication interface.
 * 2. Handle Login and Registration tab switching.
 * 3. Synchronize active tab states and visible panels.
 * 4. Initialize authentication form handlers.
 * 5. Provide basic keyboard accessibility.
 *
 * Dependencies:
 * - modules/authHandlers.js
 *
 * Usage:
 * Loaded through type="module" from index.html.
 */

import {
    initializeAuthHandlers
} from "./modules/authHandlers.js";


// =========================================
// 1. DOM ELEMENTS
// =========================================

const loginTab = document.getElementById("login-tab");
const registerTab = document.getElementById("register-tab");

const loginPanel = document.getElementById("login-panel");
const registerPanel = document.getElementById("register-panel");

const authTitle = document.getElementById("auth-title");
const authSubtitle = document.getElementById("auth-subtitle");


// =========================================
// 2. INITIALIZATION
// =========================================

function initializeAuthPage() {
    const requiredElements = [
        loginTab,
        registerTab,
        loginPanel,
        registerPanel,
        authTitle,
        authSubtitle
    ];

    const hasMissingElement = requiredElements.some(
        (element) => !element
    );

    if (hasMissingElement) {
        console.error(
            "SFIP initialization failed: Authentication elements are missing."
        );
        return;
    }

    // Initialize default Login view.
    switchAuthTab("login");

    // Register tab interactions.
    loginTab.addEventListener("click", () => {
        switchAuthTab("login");
    });

    registerTab.addEventListener("click", () => {
        switchAuthTab("register");
    });

    // Keyboard navigation between tabs.
    loginTab.addEventListener("keydown", handleTabKeyboard);
    registerTab.addEventListener("keydown", handleTabKeyboard);

    // Initialize actual Login/Register submission handlers.
    initializeAuthHandlers();

    console.info("SFIP authentication interface initialized.");
}


// =========================================
// 3. TAB SWITCHING
// =========================================

function switchAuthTab(tab) {
    const isLogin = tab === "login";

    // Update active tab appearance.
    loginTab.classList.toggle("active", isLogin);
    registerTab.classList.toggle("active", !isLogin);

    // Synchronize accessibility attributes.
    loginTab.setAttribute(
        "aria-selected",
        String(isLogin)
    );

    registerTab.setAttribute(
        "aria-selected",
        String(!isLogin)
    );

    loginTab.tabIndex = isLogin ? 0 : -1;
    registerTab.tabIndex = isLogin ? -1 : 0;

    // Display the selected form.
    loginPanel.hidden = !isLogin;
    registerPanel.hidden = isLogin;

    // Update page heading.
    authTitle.textContent = isLogin
        ? "Welcome back"
        : "Create your account";

    authSubtitle.textContent = isLogin
        ? "Sign in to continue your startup journey."
        : "Start making more informed startup decisions.";

    // Clear previous feedback messages.
    clearFormMessage("login-form");
    clearFormMessage("register-form");
}


// =========================================
// 4. KEYBOARD ACCESSIBILITY
// =========================================

function handleTabKeyboard(event) {
    if (
        event.key !== "ArrowLeft" &&
        event.key !== "ArrowRight"
    ) {
        return;
    }

    event.preventDefault();

    const nextTab = event.currentTarget === loginTab
        ? registerTab
        : loginTab;

    switchAuthTab(
        nextTab === loginTab ? "login" : "register"
    );

    nextTab.focus();
}


// =========================================
// 5. FORM FEEDBACK
// =========================================

function clearFormMessage(formId) {
    const form = document.getElementById(formId);

    if (!form) {
        return;
    }

    const messageElement = form.querySelector(".form-message");

    if (!messageElement) {
        return;
    }

    messageElement.textContent = "";
    messageElement.className = "form-message";
}


// =========================================
// 6. APPLICATION ENTRY POINT
// =========================================

initializeAuthPage();
