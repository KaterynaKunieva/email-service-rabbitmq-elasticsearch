package org.kunievakateryna.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kunievakateryna.data.EmailHistory;
import org.kunievakateryna.data.EmailStatus;
import org.kunievakateryna.repository.EmailHistoryRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled service responsible for resending failed emails
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailRetryScheduler {

    private final EmailHistoryRepository repository;
    private final EmailService emailService;

    /**
     * Scheduled entry point that finds failed emails and retries sending them
     */
    @Scheduled(
            fixedDelayString = "${app.scheduling.retry-delay:300000}",
            initialDelayString = "${app.scheduling.initial-delay:0}"
    )
    public void retryFailedEmails() {
        log.info("Starting email resend scheduler");

        List<EmailHistory> failedEmails = findFailedEmails();
        log.info("Found {} emails with ERROR status to resend", failedEmails.size());

        if (failedEmails.isEmpty()) {
            return;
        }

        for (EmailHistory email : failedEmails) {
            processRetry(email);
        }

        log.info("Email resend scheduler finished");
    }

    /**
     * Retrieves all emails that have ERROR status.
     *
     * @return list of failed emails
     */
    private List<EmailHistory> findFailedEmails() {
        return repository.findByStatus(EmailStatus.ERROR);
    }

    /**
     * Processes a single email retry attempt
     *
     * @param email email history record
     */
    private void processRetry(EmailHistory email) {
        try {
            resendEmail(email);
            markAsSent(email);
        } catch (Exception e) {
            markAsError(email, e);
        } finally {
            updateAttemptMetadata(email);
            repository.save(email);
        }
    }

    /**
     * Sends the email using the email service
     *
     * @param email email history record
     */
    private void resendEmail(EmailHistory email) {
        log.info(
                "Resending email ID: {} for recipient: {}",
                email.getId(),
                email.getRecipient()
        );

        emailService.sendSimpleEmail(
                email.getRecipient(),
                email.getSubject(),
                email.getContent()
        );
    }

    /**
     * Updates email status after successful resend
     *
     * @param email email history record
     */
    private void markAsSent(EmailHistory email) {
        email.setStatus(EmailStatus.SENT);
        email.setErrorMessage(null);

        log.info("Email ID: {} resent successfully", email.getId());
    }

    /**
     * Updates email error information after failed resend
     *
     * @param email email history record
     * @param exception exception that occurred during resend
     */
    private void markAsError(EmailHistory email, Exception exception) {
        email.setErrorMessage(
                exception.getClass().getSimpleName() + ": " + exception.getMessage()
        );

        log.error(
                "Resending failed for email ID {}: {}",
                email.getId(),
                exception.getMessage()
        );
    }

    /**
     * Updates attempt count and last attempt timestamp
     *
     * @param email email history record
     */
    private void updateAttemptMetadata(EmailHistory email) {
        email.setAttempts(email.getAttempts() + 1);
        email.setLastAttemptTime(LocalDateTime.now());
    }
}
