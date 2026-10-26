/**
 * File Name: dashboardHandlers.js
 * File Path: static/js/modules/dashboardHandlers.js
 * Project: Startup Failure Intelligence Platform (SFIP)
 *
 * Description:
 * Handles startup analysis, report rendering,
 * and analysis history.
 *
 * Dependencies:
 * - apiClient.js
 *
 * API Endpoints:
 * POST /api/sessions
 * GET  /api/sessions
 * GET  /api/sessions/{sessionId}
 *
 * Note:
 * Analysis is expected to complete synchronously
 * according to the current frontend contract.
 */

// =========================================
// 1. IMPORTS
// =========================================

import { apiRequest } from "../api/apiClient.js";

// =========================================
// 2. CONFIGURATION
// =========================================

const MIN_IDEA_LENGTH = 20;
const MAX_IDEA_LENGTH = 10000;

// =========================================
// 3. SESSION RESPONSE HANDLING
// =========================================

/**
 * Extracts the session from the API response.
 *
 * Supports:
 * { session: { ... } }
 *
 * Also supports:
 * { sessionId: ..., ... }
 */

function extractSession(response) {
    const data = response?.data ?? response;
    const session = data?.session ?? data;

    if (
        !session ||
        typeof session !== "object" ||
        session.sessionId === undefined ||
        session.sessionId === null ||
        String(session.sessionId).trim() === ""
    ) {
        throw new Error("Invalid session response received.");
    }

    return session;
}

// =========================================
// 4. STARTUP IDEA VALIDATION
// =========================================

function validateStartupIdea(startupIdea) {
    if (typeof startupIdea !== "string") {
        throw new Error("Please enter your startup idea.");
    }

    const idea = startupIdea.trim();

    if (idea.length < MIN_IDEA_LENGTH) {
        throw new Error(
            `Startup idea must contain at least ${MIN_IDEA_LENGTH} characters.`
        );
    }

    if (idea.length > MAX_IDEA_LENGTH) {
        throw new Error(
            `Startup idea cannot exceed ${MAX_IDEA_LENGTH} characters.`
        );
    }

    return idea;
}

// =========================================
// 5. CREATE ANALYSIS SESSION
// =========================================

export async function createAnalysisSession(startupIdea) {
    const idea = validateStartupIdea(startupIdea);

    const response = await apiRequest("/api/sessions", {
        method: "POST",
        body: {
            startupIdea: idea
        }
    });

    return extractSession(response);
}

// =========================================
// 6. GET ANALYSIS SESSION
// =========================================

export async function getAnalysisSession(sessionId) {
    const id = Number(sessionId);

    if (!Number.isSafeInteger(id) || id <= 0) {
        throw new Error("Invalid analysis session ID.");
    }

    const response = await apiRequest(
        `/api/sessions/${id}`,
        {
            method: "GET"
        }
    );

    return extractSession(response);
}

// =========================================
// 7. FETCH ANALYSIS HISTORY
// =========================================

export async function fetchAnalysisHistory() {
    const response = await apiRequest("/api/sessions", {
        method: "GET"
    });

    const data = response?.data ?? response;

    if (!Array.isArray(data?.sessions)) {
        throw new Error(
            "Invalid analysis history response."
        );
    }

    return data.sessions;
}

// =========================================
// 8. DASHBOARD MESSAGE
// =========================================

/**
 * Displays analysis feedback.
 *
 * HTML initially has hidden=true.
 * Remove hidden when showing any message.
 */

function showDashboardMessage(message, type = "info") {
    const messageElement = document.getElementById(
        "analysis-message"
    );

    if (!messageElement) {
        console.error(
            "Analysis message element not found."
        );
        return;
    }

    messageElement.textContent = message;
    messageElement.dataset.type = type;
    messageElement.hidden = false;
}

// =========================================
// 9. REPORT RENDERING
// =========================================

/**
 * Displays an analysis report safely.
 *
 * AI content is rendered using textContent.
 * No AI-generated HTML is executed.
 */

export function renderReport(session) {
    const reportContainer = document.getElementById(
        "analysis-report"
    );

    if (!reportContainer) {
        throw new Error(
            "Analysis report container not found. Check dashboard.html."
        );
    }

    reportContainer.replaceChildren();

    const status = String(
        session.status ?? "UNKNOWN"
    ).toUpperCase();

    // Handle incomplete or failed analysis.
    if (status !== "COMPLETED") {
        const statusMessage = document.createElement("p");

        statusMessage.textContent =
            status === "FAILED"
                ? "Analysis failed. Please try again."
                : `Analysis status: ${status}`;

        reportContainer.appendChild(statusMessage);
        reportContainer.hidden = false;

        return;
    }

    // Handle missing report.
    if (
        typeof session.aiReport !== "string" ||
        !session.aiReport.trim()
    ) {
        const emptyMessage = document.createElement("p");

        emptyMessage.textContent =
            "Analysis completed, but the report is unavailable.";

        reportContainer.appendChild(emptyMessage);
        reportContainer.hidden = false;

        return;
    }

    // Report heading.
    const heading = document.createElement("h2");
    heading.textContent = "Startup Analysis Report";

    // Report content.
    const report = document.createElement("pre");

    report.className = "analysis-report-content";
    report.textContent = session.aiReport;
    report.style.whiteSpace = "pre-wrap";
    report.style.overflowWrap = "anywhere";

    reportContainer.append(heading, report);
    reportContainer.hidden = false;
}

/**
 * Scrolls to the report when it is displayed.
 */

function scrollToReport() {
    const reportSection = document.getElementById(
        "analysis-report-section"
    );

    if (!reportSection) {
        return;
    }

    reportSection.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}

// =========================================
// 10. HANDLE ANALYSIS SUBMISSION
// =========================================

export async function handleAnalysisSubmit(event) {
    event?.preventDefault();

    const ideaInput = document.getElementById("startup-idea");
    const analyzeButton = document.getElementById("analyze-btn");

    if (!ideaInput) {
        showDashboardMessage(
            "Startup idea input was not found.",
            "error"
        );
        return;
    }

    // Prevent duplicate submissions.
    if (analyzeButton?.disabled) {
        return;
    }

    // Validate before sending the request.
    let startupIdea;

    try {
        startupIdea = validateStartupIdea(ideaInput.value);
    } catch (error) {
        showDashboardMessage(
            error?.message || "Invalid startup idea.",
            "error"
        );

        ideaInput.focus();
        return;
    }

    // Preserve the original button appearance.
    const originalButtonHTML = analyzeButton?.innerHTML;

    if (analyzeButton) {
        analyzeButton.disabled = true;
        analyzeButton.textContent = "Analyzing...";
    }

    showDashboardMessage(
        "Analyzing your startup idea...",
        "info"
    );

    let session;

    // STEP 1: Submit the analysis request.
    try {
        session = await createAnalysisSession(startupIdea);
    } catch (error) {
        console.error(
            "Analysis submission error:",
            error
        );

        const message =
            error?.status === 0
                ? "Unable to connect to the server. Check your backend and network."
                : error?.message ||
                  "Analysis request failed. Please try again.";

        // The session may already exist even if the request failed.
        // Refresh history, but preserve the original error message.
        try {
            await loadAnalysisHistory();
        } catch (historyError) {
            console.error(
                "History refresh error:",
                historyError
            );
        }

        showDashboardMessage(message, "error");
        return;
    } finally {
        // Restore the button once the request settles.
        if (analyzeButton) {
            analyzeButton.disabled = false;

            if (originalButtonHTML !== undefined) {
                analyzeButton.innerHTML = originalButtonHTML;
            } else {
                analyzeButton.textContent = "Analyze Startup Idea";
            }
        }
    }

    // STEP 2: Read the backend's analysis status.
    const status = String(
        session.status ?? "UNKNOWN"
    ).toUpperCase();

    // STEP 3: Render the report independently of analysis status.
    let reportRenderFailed = false;

    try {
        renderReport(session);
        scrollToReport();
    } catch (error) {
        reportRenderFailed = true;

        console.error(
            "Report rendering error:",
            error
        );
    }

    // STEP 4: Display feedback without confusing UI errors
    // with the backend's actual analysis result.
    if (reportRenderFailed) {
        showDashboardMessage(
            status === "COMPLETED"
                ? "Analysis completed, but the report could not be displayed."
                : `Analysis status: ${status}. Report display failed.`,
            "error"
        );
    } else if (status === "COMPLETED") {
        showDashboardMessage(
            "Startup analysis completed successfully.",
            "success"
        );
    } else if (status === "FAILED") {
        showDashboardMessage(
            "Analysis failed. Please try again.",
            "error"
        );
    } else {
        showDashboardMessage(
            `Analysis status: ${status}`,
            "info"
        );
    }

    // STEP 5: Refresh history independently.
    // History errors must not change the analysis result.
    await loadAnalysisHistory();
}

// =========================================
// 11. OPEN EXISTING ANALYSIS
// =========================================

async function handleOpenAnalysis(sessionId) {
    showDashboardMessage(
        "Loading analysis report...",
        "info"
    );

    try {
        const session = await getAnalysisSession(sessionId);

        renderReport(session);
        scrollToReport();

        const status = String(
            session.status ?? "UNKNOWN"
        ).toUpperCase();

        if (status === "COMPLETED") {
            showDashboardMessage(
                "Analysis report loaded successfully.",
                "success"
            );
        } else {
            showDashboardMessage(
                status === "FAILED"
                    ? "This analysis failed."
                    : `Analysis status: ${status}`,
                status === "FAILED" ? "error" : "info"
            );
        }
    } catch (error) {
        console.error(
            "Report retrieval error:",
            error
        );

        showDashboardMessage(
            error?.message ||
                "Unable to load analysis report.",
            "error"
        );
    }
}

// =========================================
// 12. RENDER ANALYSIS HISTORY
// =========================================

function renderAnalysisHistory(sessions) {
    const historyContainer = document.getElementById(
        "history-container"
    );

    if (!historyContainer) {
        console.error(
            "History container not found."
        );
        return;
    }

    historyContainer.replaceChildren();

    // Empty history.
    if (sessions.length === 0) {
        const emptyMessage = document.createElement("p");

        emptyMessage.textContent =
            "No analysis history found.";

        historyContainer.appendChild(emptyMessage);
        return;
    }

    sessions.forEach((session) => {
        const item = document.createElement("div");
        item.className = "history-item";

        // Startup idea.
        const idea = document.createElement("p");

        idea.textContent =
            session.startupIdea ?? "Untitled startup";

        // Analysis status.
        const status = document.createElement("p");

        status.textContent =
            `Status: ${session.status ?? "UNKNOWN"}`;

        // Creation date.
        const date = document.createElement("p");

        if (session.createdAt) {
            const parsedDate = new Date(session.createdAt);

            date.textContent = Number.isNaN(
                parsedDate.getTime()
            )
                ? "Creation date unavailable"
                : `Created: ${parsedDate.toLocaleString()}`;
        } else {
            date.textContent = "Creation date unavailable";
        }

        item.append(idea, status, date);

        // Open report button.
        if (session.sessionId != null) {
            const openButton = document.createElement(
                "button"
            );

            openButton.type = "button";
            openButton.className = "open-analysis-btn";
            openButton.textContent = "Open Analysis";

            openButton.addEventListener("click", () => {
                handleOpenAnalysis(session.sessionId);
            });

            item.appendChild(openButton);
        }

        historyContainer.appendChild(item);
    });
}

// =========================================
// 13. LOAD ANALYSIS HISTORY
// =========================================

export async function loadAnalysisHistory() {
    const historyContainer = document.getElementById(
        "history-container"
    );

    try {
        const sessions = await fetchAnalysisHistory();
        renderAnalysisHistory(sessions);
    } catch (error) {
        console.error(
            "Analysis history error:",
            error
        );

        if (historyContainer) {
            historyContainer.replaceChildren();

            const message = document.createElement("p");

            message.textContent =
                "Unable to load analysis history.";

            historyContainer.appendChild(message);
        }
    }
}