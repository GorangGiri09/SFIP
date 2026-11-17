
/*
 * ============================================================
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : StartupSessionService.java
 * Package      : com.sfip.backend.service
 * File Path    : src/main/java/com/sfip/backend/service/StartupSessionService.java
 *
 * Purpose:
 * Handles business logic and lifecycle management of startup
 * analysis sessions.
 *
 * Responsibilities:
 * 1. Creates startup analysis sessions.
 * 2. Manages PENDING, PROCESSING, COMPLETED and FAILED states.
 * 3. Executes AI and ML analysis outside database transactions.
 * 4. Persists ML results and Groq-generated reports.
 * 5. Handles analysis failures and independent failure persistence.
 * 6. Retrieves user analysis history.
 * 7. Enforces session ownership.
 * 8. Maintains transactional consistency.
 *
 * Analysis Workflow:
 * PENDING -> PROCESSING -> COMPLETED
 *
 * Failure Workflow:
 * PENDING -> PROCESSING -> FAILED
 *
 * Persistence:
 * - ml_result stores serialized ML analysis JSON.
 * - ai_report stores the final Groq-generated report.
 *
 * Transaction Strategy:
 * - Database transactions are used for session state changes.
 * - External AI/ML calls execute without an open DB transaction.
 * - ML result, AI report and COMPLETED status are saved atomically.
 * - FAILED status is persisted independently on processing errors.
 *
 * ============================================================
 */

package com.sfip.backend.service;

import com.sfip.backend.entity.AnalysisStatus;
import com.sfip.backend.entity.StartupSession;
import com.sfip.backend.entity.User;
import com.sfip.backend.integration.analysis.AnalysisResult;
import com.sfip.backend.repository.StartupSessionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;


/**
 * Application service responsible for startup analysis
 * session creation, processing and persistence.
 */
@Service
@Transactional(readOnly = true)
public class StartupSessionService {

    private static final Logger logger =
            LoggerFactory.getLogger(StartupSessionService.class);

    private final StartupSessionRepository startupSessionRepository;

    private final StartupAnalysisService startupAnalysisService;

    private final TransactionTemplate transactionTemplate;


    // --------------------------------------------------------
    // Constructor
    // --------------------------------------------------------

    /**
     * Initializes session management dependencies.
     *
     * @param startupSessionRepository session persistence layer
     * @param startupAnalysisService startup analysis orchestrator
     * @param transactionTemplate transaction management
     */
    public StartupSessionService(
            StartupSessionRepository startupSessionRepository,
            StartupAnalysisService startupAnalysisService,
            TransactionTemplate transactionTemplate) {

        this.startupSessionRepository = startupSessionRepository;
        this.startupAnalysisService = startupAnalysisService;
        this.transactionTemplate = transactionTemplate;
    }


    // --------------------------------------------------------
    // Create Startup Session
    // Initial status: PENDING
    // --------------------------------------------------------

    /**
     * Creates a new startup analysis session.
     *
     * @param user authenticated application user
     * @param startupIdea original startup description
     * @return persisted startup session
     */
    @Transactional
    public StartupSession createSession(
            User user,
            String startupIdea) {

        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException(
                    "A valid user is required."
            );
        }

        if (startupIdea == null || startupIdea.isBlank()) {
            throw new IllegalArgumentException(
                    "Startup idea cannot be empty."
            );
        }

        String normalizedIdea = startupIdea.trim();

        if (normalizedIdea.length() < 20) {
            throw new IllegalArgumentException(
                    "Startup idea must contain at least 20 characters."
            );
        }

        if (normalizedIdea.length() > 10000) {
            throw new IllegalArgumentException(
                    "Startup idea cannot exceed 10000 characters."
            );
        }

        StartupSession session =
                new StartupSession(user, normalizedIdea);

        session.setStatus(AnalysisStatus.PENDING);

        return startupSessionRepository.save(session);
    }


    // --------------------------------------------------------
    // Process Startup Analysis
    // --------------------------------------------------------

    /**
     * Processes a pending startup analysis session.
     *
     * Processing steps:
     *
     * 1. Fetch session and mark PROCESSING.
     * 2. Execute Groq extraction, ML analysis and final report.
     * 3. Persist ML JSON and final report.
     * 4. Mark session COMPLETED.
     * 5. On failure, independently mark session FAILED.
     *
     * External AI and ML requests execute outside database
     * transactions to prevent long-running transactions.
     *
     * @param sessionId target session ID
     * @param userId authenticated user ID
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processAnalysis(
            Long sessionId,
            Long userId) {

        // ----------------------------------------------------
        // Transaction 1: Mark session PROCESSING
        // ----------------------------------------------------

        String startupIdea = transactionTemplate.execute(status -> {

            StartupSession session =
                    getUserSession(sessionId, userId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Analysis session not found."
                                    )
                            );

            if (session.getStatus() != AnalysisStatus.PENDING) {
                throw new IllegalStateException(
                        "Only pending sessions can be processed."
                );
            }

            session.setStatus(AnalysisStatus.PROCESSING);

            startupSessionRepository.save(session);

            return session.getStartupIdea();
        });


        try {

            if (startupIdea == null || startupIdea.isBlank()) {
                throw new IllegalStateException(
                        "Startup idea is unavailable."
                );
            }


            // ------------------------------------------------
            // Execute AI and ML workflow outside DB transaction
            // ------------------------------------------------

            AnalysisResult result =
                    startupAnalysisService.analyzeStartup(startupIdea);

            if (result == null
                    || result.report() == null
                    || result.report().isBlank()) {

                throw new IllegalStateException(
                        "Analysis provider returned an empty report."
                );
            }


            // ------------------------------------------------
            // Transaction 2: Persist Analysis Results
            // ------------------------------------------------

            transactionTemplate.executeWithoutResult(status -> {

                StartupSession session =
                        getUserSession(sessionId, userId)
                                .orElseThrow(() ->
                                        new IllegalArgumentException(
                                                "Analysis session not found."
                                        )
                                );

                if (session.getStatus()
                        != AnalysisStatus.PROCESSING) {

                    throw new IllegalStateException(
                            "Analysis session is no longer processing."
                    );
                }

                // Persist serialized ML response.
                session.setMlResult(result.mlResult());

                // Persist final Groq-generated report.
                session.setAiReport(result.report());

                // Mark analysis as completed.
                session.setStatus(AnalysisStatus.COMPLETED);

                startupSessionRepository.save(session);
            });

            logger.info(
                    "Startup analysis session {} completed successfully.",
                    sessionId
            );


        } catch (RuntimeException exception) {

            // ------------------------------------------------
            // Transaction 3: Persist FAILED Status
            // ------------------------------------------------

            try {

                transactionTemplate.executeWithoutResult(status -> {

                    StartupSession session =
                            getUserSession(sessionId, userId)
                                    .orElseThrow(() ->
                                            new IllegalArgumentException(
                                                    "Analysis session not found."
                                            )
                                    );

                    if (session.getStatus()
                            == AnalysisStatus.PROCESSING) {

                        session.setStatus(AnalysisStatus.FAILED);

                        startupSessionRepository.save(session);
                    }
                });

            } catch (RuntimeException persistenceException) {

                // Preserve the original analysis failure.
                exception.addSuppressed(persistenceException);

                logger.error(
                        "Unable to persist FAILED status for session {}.",
                        sessionId,
                        persistenceException
                );
            }

            logger.error(
                    "Startup analysis session {} failed.",
                    sessionId,
                    exception
            );

            throw new IllegalStateException(
                    "Startup analysis could not be completed.",
                    exception
            );
        }
    }


    // --------------------------------------------------------
    // Retrieve User Analysis History
    // --------------------------------------------------------

    /**
     * Retrieves all startup analysis sessions belonging
     * to the authenticated user.
     *
     * Sessions are ordered newest first.
     *
     * @param userId authenticated user ID
     * @return user's analysis history
     */
    public List<StartupSession> getUserAnalysisHistory(
            Long userId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID is required."
            );
        }

        return startupSessionRepository
                .findAllByUser_IdOrderByCreatedAtDesc(userId);
    }


    // --------------------------------------------------------
    // Retrieve Individual User Session
    // --------------------------------------------------------

    /**
     * Retrieves a session only if it belongs to the specified user.
     *
     * @param sessionId target session ID
     * @param userId authenticated user ID
     * @return matching session, if available
     */
    public Optional<StartupSession> getUserSession(
            Long sessionId,
            Long userId) {

        if (sessionId == null
                || sessionId <= 0
                || userId == null) {

            throw new IllegalArgumentException(
                    "Valid session ID and user ID are required."
            );
        }

        return startupSessionRepository
                .findByIdAndUser_Id(sessionId, userId);
    }
}
