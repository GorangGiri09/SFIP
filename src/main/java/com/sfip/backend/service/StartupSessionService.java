
/*
 * ============================================================
 * Project Name : SFIP (Startup Failure Intelligence Platform)
 * File Name    : StartupSessionService.java
 * Package      : com.sfip.backend.service
 *
 * Purpose:
 * Handles business logic for startup analysis sessions.
 *
 * Responsibilities:
 * 1. Creates startup analysis sessions.
 * 2. Processes startup analysis using StartupAnalysisService.
 * 3. Updates analysis status and saves generated reports.
 * 4. Retrieves analysis history for a user.
 * 5. Retrieves individual sessions with ownership verification.
 * 6. Maintains transactional consistency.
 *
 * ============================================================
 */

package com.sfip.backend.service;

import com.sfip.backend.entity.AnalysisStatus;
import com.sfip.backend.entity.StartupSession;
import com.sfip.backend.entity.User;
import com.sfip.backend.integration.analysis.AnalysisResult;
import com.sfip.backend.repository.StartupSessionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class StartupSessionService {

    private final StartupSessionRepository startupSessionRepository;
    private final StartupAnalysisService startupAnalysisService;
    private final TransactionTemplate transactionTemplate;

    // Constructor injection.
    public StartupSessionService(
            StartupSessionRepository startupSessionRepository,
            StartupAnalysisService startupAnalysisService,
            TransactionTemplate transactionTemplate) {

        this.startupSessionRepository = startupSessionRepository;
        this.startupAnalysisService = startupAnalysisService;
        this.transactionTemplate = transactionTemplate;
    }

    // --------------------------------------------------------
    // Create a new startup analysis session.
    // Initial status: PENDING
    // --------------------------------------------------------

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
    // Process a pending analysis session.
    //
    // 1. Mark session PROCESSING.
    // 2. Call analysis provider outside DB transaction.
    // 3. Save report and mark COMPLETED.
    // 4. On processing failure, mark FAILED.
    // --------------------------------------------------------

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processAnalysis(Long sessionId, Long userId) {

        // Transaction 1:
        // Fetch session and mark PROCESSING.
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

            // AI call runs without an open DB transaction.
            AnalysisResult result =
                    startupAnalysisService.analyzeStartup(startupIdea);

            if (result == null
                    || result.report() == null
                    || result.report().isBlank()) {

                throw new IllegalStateException(
                        "Analysis provider returned an empty report."
                );
            }

            // Transaction 2:
            // Persist report and COMPLETED status atomically.
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

                session.setMlResult(null);
                session.setAiReport(result.report());
                session.setStatus(AnalysisStatus.COMPLETED);

                startupSessionRepository.save(session);
            });

        } catch (RuntimeException exception) {

            // Transaction 3:
            // Attempt to persist FAILED status independently.
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

            }

            throw new IllegalStateException(
                    "Startup analysis could not be completed.",
                    exception
            );
        }
    }

    // --------------------------------------------------------
    // Retrieve all analysis sessions belonging to a user.
    // Newest sessions appear first.
    // --------------------------------------------------------

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
    // Retrieve a session only if it belongs to the given user.
    // --------------------------------------------------------

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
