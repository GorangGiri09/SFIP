/*
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : apiClient.js
 * File Path    : src/main/resources/static/js/api/apiClient.js
 * Package      : N/A (Frontend JavaScript ES Module)
 *
 * Purpose:
 * Provides a reusable HTTP client for backend communication.
 *
 * Responsibilities:
 * 1. Send HTTP requests to the Spring Boot backend.
 * 2. Handle JSON request and response bodies.
 * 3. Attach JWT Bearer tokens when available.
 * 4. Preserve authentication across same-tab navigation.
 * 5. Handle HTTP errors and network failures.
 * 6. Support optional request timeouts.
 * 7. Provide consistent API error handling.
 *
 * Usage:
 * Import apiRequest() wherever backend API calls are required.
 */

// =========================================
// 1. API CONFIGURATION
// =========================================

// Frontend and backend share the same application origin.
const API_BASE_URL = "";

// Session-scoped token storage.
const TOKEN_STORAGE_KEY = "sfip_access_token";

let accessToken = null;


// =========================================
// 2. TOKEN MANAGEMENT
// =========================================

/**
 * Returns the stored access token.
 *
 * Loads from sessionStorage when required.
 *
 * @returns {string|null}
 */
export function getAccessToken() {
    if (accessToken) {
        return accessToken;
    }

    try {
        const token = sessionStorage.getItem(
            TOKEN_STORAGE_KEY
        );

        accessToken =
            typeof token === "string" && token.trim()
                ? token.trim()
                : null;

        return accessToken;
    } catch {
        return null;
    }
}

/**
 * Sets the JWT access token.
 *
 * @param {string|null} token
 */
export function setAccessToken(token) {
    const normalizedToken =
        typeof token === "string" && token.trim()
            ? token.trim()
            : null;

    if (!normalizedToken) {
        clearAccessToken();
        return;
    }

    try {
        sessionStorage.setItem(
            TOKEN_STORAGE_KEY,
            normalizedToken
        );

        accessToken = normalizedToken;
    } catch {
        accessToken = null;

        throw new Error(
            "Unable to maintain your login session. Please try again."
        );
    }
}

/**
 * Clears the current authentication token.
 */
export function clearAccessToken() {
    accessToken = null;

    try {
        sessionStorage.removeItem(
            TOKEN_STORAGE_KEY
        );
    } catch {
        // In-memory token is already cleared.
    }
}


// =========================================
// 3. API ERROR
// =========================================

/**
 * Represents an unsuccessful API request.
 *
 * status:
 *   0 = Network failure / client-side failure
 *   HTTP status = Server responded with an error
 *
 * data:
 *   Parsed response body, if available.
 */
export class ApiError extends Error {
    constructor(
        message,
        status = 0,
        data = null,
        code = null
    ) {
        super(message);

        this.name = "ApiError";
        this.status = status;
        this.data = data;
        this.code = code;
    }
}


// =========================================
// 4. RESPONSE PARSING
// =========================================

/**
 * Safely extracts response data.
 *
 * Supports JSON and text responses.
 *
 * @param {Response} response
 * @returns {Promise<any>}
 */
async function parseResponse(response) {
    if (response.status === 204) {
        return null;
    }

    let responseText;

    try {
        responseText = await response.text();
    } catch {
        throw new ApiError(
            "Unable to read the server response.",
            response.status
        );
    }

    if (!responseText) {
        return null;
    }

    const contentType =
        response.headers.get("content-type") || "";

    const isJson =
        contentType.includes("application/json") ||
        contentType.includes("+json");

    if (!isJson) {
        return responseText;
    }

    try {
        return JSON.parse(responseText);
    } catch {
        throw new ApiError(
            "The server returned invalid JSON.",
            response.status
        );
    }
}


// =========================================
// 5. ERROR MESSAGE EXTRACTION
// =========================================

/**
 * Extracts a meaningful error message.
 *
 * Supports common Spring Boot error formats.
 *
 * @param {any} data
 * @returns {string}
 */
function extractErrorMessage(data) {
    const fallbackMessage =
        "Something went wrong. Please try again.";

    if (typeof data === "string") {
        return data.trim() || fallbackMessage;
    }

    if (!data || typeof data !== "object") {
        return fallbackMessage;
    }

    const message =
        data.message ??
        data.detail ??
        data.error;

    return typeof message === "string" && message.trim()
        ? message.trim()
        : fallbackMessage;
}


// =========================================
// 6. API REQUEST
// =========================================

/**
 * Sends an HTTP request to the backend.
 *
 * @param {string} path API endpoint path
 * @param {object} options Request configuration
 *
 * Supported options:
 * - method
 * - body
 * - headers
 * - signal
 * - timeoutMs
 *
 * timeoutMs:
 * - 0 = No client-side timeout (default)
 * - Positive number = Timeout in milliseconds
 *
 * @returns {Promise<any>}
 */
export async function apiRequest(path, options = {}) {
    const {
        method = "GET",
        body,
        headers = {},
        signal,
        timeoutMs = 0
    } = options;

    // =========================================
    // STEP 1: VALIDATE REQUEST
    // =========================================

    if (
        typeof path !== "string" ||
        !path.startsWith("/") ||
        path.startsWith("//") ||
        path.includes("\\") ||
        /[\r\n]/.test(path)
    ) {
        throw new ApiError(
            "Invalid API endpoint."
        );
    }

    if (
        typeof method !== "string" ||
        !/^[A-Za-z]+$/.test(method)
    ) {
        throw new ApiError(
            "Invalid HTTP method."
        );
    }

    if (
        !Number.isFinite(timeoutMs) ||
        timeoutMs < 0
    ) {
        throw new ApiError(
            "Invalid request timeout."
        );
    }

    if (
        signal !== undefined &&
        (
            typeof signal !== "object" ||
            typeof signal.aborted !== "boolean" ||
            typeof signal.addEventListener !== "function"
        )
    ) {
        throw new ApiError(
            "Invalid request signal."
        );
    }

    if (
        headers === null ||
        typeof headers !== "object"
    ) {
        throw new ApiError(
            "Invalid request headers."
        );
    }

    const normalizedMethod = method.toUpperCase();

    // =========================================
    // STEP 2: PREPARE REQUEST HEADERS
    // =========================================

    let requestHeaders;

    try {
        requestHeaders = new Headers(headers);
    } catch {
        throw new ApiError(
            "Invalid request headers."
        );
    }

    requestHeaders.set(
        "Accept",
        "application/json"
    );

    // Attach JWT when available.
    const token = getAccessToken();

    if (token) {
        requestHeaders.set(
            "Authorization",
            `Bearer ${token}`
        );
    }

    // =========================================
    // STEP 3: PREPARE REQUEST BODY
    // =========================================

    let requestBody;

    if (body !== undefined) {
        if (
            normalizedMethod === "GET" ||
            normalizedMethod === "HEAD"
        ) {
            throw new ApiError(
                `${normalizedMethod} requests cannot contain a JSON body.`
            );
        }

        try {
            requestBody = JSON.stringify(body);
        } catch {
            throw new ApiError(
                "Unable to prepare the request body."
            );
        }

        if (requestBody === undefined) {
            throw new ApiError(
                "Invalid request body."
            );
        }

        requestHeaders.set(
            "Content-Type",
            "application/json"
        );
    }

    // =========================================
    // STEP 4: PREPARE ABORT HANDLING
    // =========================================

    let controller = null;
    let timeoutId = null;
    let timedOut = false;

    let removeAbortListener = null;

    if (timeoutMs > 0) {
        controller = new AbortController();

        // Respect caller cancellation.
        const abortRequest = () => {
            controller.abort();
        };

        if (signal?.aborted) {
            throw new ApiError(
                "Request was cancelled.",
                0,
                null,
                "REQUEST_ABORTED"
            );
        }

        if (signal) {
            signal.addEventListener(
                "abort",
                abortRequest,
                { once: true }
            );

            removeAbortListener = () => {
                signal.removeEventListener(
                    "abort",
                    abortRequest
                );
            };
        }

        timeoutId = setTimeout(() => {
            timedOut = true;
            controller.abort();
        }, timeoutMs);
    }

    const requestSignal = controller
        ? controller.signal
        : signal;

    const url = `${API_BASE_URL}${path}`;

    // =========================================
    // STEP 5: SEND REQUEST
    // =========================================

    try {
        const response = await fetch(url, {
            method: normalizedMethod,
            headers: requestHeaders,
            body: requestBody,
            signal: requestSignal
        });

        // =========================================
        // STEP 6: PARSE RESPONSE
        // =========================================

        const responseData = await parseResponse(
            response
        );

        // =========================================
        // STEP 7: HANDLE HTTP ERRORS
        // =========================================

        if (!response.ok) {
            let message = extractErrorMessage(
                responseData
            );

            if (response.status === 401) {
                message =
                    "Session expired or authentication failed.";
            } else if (response.status === 403) {
                message =
                    "You do not have permission for this action.";
            } else if (response.status === 404) {
                message =
                    "Requested resource was not found.";
            } else if (response.status >= 500) {
                message =
                    "Server error. Please try again later.";
            }

            throw new ApiError(
                message,
                response.status,
                responseData,
                "HTTP_ERROR"
            );
        }

        // =========================================
        // STEP 8: RETURN RESPONSE
        // =========================================

        return responseData;

    } catch (error) {
        // Preserve known API errors.
        if (error instanceof ApiError) {
            throw error;
        }

        // Distinguish timeout from manual cancellation.
        if (timedOut) {
            throw new ApiError(
                "Request timed out. The server may still be processing your request.",
                0,
                null,
                "REQUEST_TIMEOUT"
            );
        }

        if (
            signal?.aborted ||
            error?.name === "AbortError"
        ) {
            throw new ApiError(
                "Request was cancelled.",
                0,
                null,
                "REQUEST_ABORTED"
            );
        }

        // Network, fetch, or unexpected client errors.
        throw new ApiError(
            "Unable to connect to the server. Please check your connection and try again.",
            0,
            null,
            "NETWORK_ERROR"
        );

    } finally {
        // Release timeout and event listener.
        if (timeoutId !== null) {
            clearTimeout(timeoutId);
        }

        if (removeAbortListener) {
            removeAbortListener();
        }
    }
}