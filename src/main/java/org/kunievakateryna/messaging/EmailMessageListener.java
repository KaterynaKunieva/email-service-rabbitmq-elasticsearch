package org.kunievakateryna.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kunievakateryna.data.EmailHistory;
import org.kunievakateryna.data.EmailStatus;
import org.kunievakateryna.dto.EmailMessageDto;
import org.kunievakateryna.repository.EmailHistoryRepository;
import org.kunievakateryna.service.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Listener that processes incoming email messages from RabbitMQ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailMessageListener {

    private final EmailHistoryRepository repository;
    private final EmailService emailService;

    /**
     * Entry point for messages received from RabbitMQ.
     * Creates email history, sends the email, and updates the status.
     *
     * @param messageDto email data received from the queue
     */
    @RabbitListener(queues = "${app.rabbitmq.email-queue}")
    public void receiveMessage(EmailMessageDto messageDto) {
        EmailHistory history = createPendingHistory(messageDto);
        repository.save(history);

        log.info("Created history record in DB with status PENDING, ID: {}", history.getId());

        try {
            sendEmail(messageDto);
            markAsSent(history);
        } catch (Exception e) {
            markAsError(history, e);
        }
    }

    /**
     * Creates a new email history record with PENDING status
     *
     * @param messageDto email message data
     * @return initialized EmailHistory entity
     */
    private EmailHistory createPendingHistory(EmailMessageDto messageDto) {
        return EmailHistory.builder()
                .id(UUID.randomUUID().toString())
                .recipient(messageDto.getRecipient())
                .subject(messageDto.getSubject())
                .content(messageDto.getBody())
                .status(EmailStatus.PENDING)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Sends an email using the email service
     *
     * @param messageDto email message data
     */
    private void sendEmail(EmailMessageDto messageDto) {
        emailService.sendSimpleEmail(
                messageDto.getRecipient(),
                messageDto.getSubject(),
                messageDto.getBody()
        );
    }

    /**
     * Updates email history after successful sending
     *
     * @param history email history entity
     */
    private void markAsSent(EmailHistory history) {
        history.setStatus(EmailStatus.SENT);
        history.setLastAttemptTime(LocalDateTime.now());
        history.setAttempts(1);
        repository.save(history);

        log.info("Email successfully sent to: {}", history.getRecipient());
    }

    /**
     * Updates email history after a sending error
     *
     * @param history   email history entity
     * @param exception exception that occurred during sending
     */
    private void markAsError(EmailHistory history, Exception exception) {
        history.setStatus(EmailStatus.ERROR);
        history.setErrorMessage(exception.getClass().getSimpleName() + ": " + exception.getMessage());
        history.setLastAttemptTime(LocalDateTime.now());
        history.setAttempts(1);
        repository.save(history);

        log.error(
                "Failed to send email to {}. Error: {}",
                history.getRecipient(),
                exception.getMessage()
        );
    }
}
