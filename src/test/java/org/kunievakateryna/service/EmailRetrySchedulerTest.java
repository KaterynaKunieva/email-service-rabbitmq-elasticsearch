package org.kunievakateryna.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kunievakateryna.config.TestElasticsearchConfiguration;
import org.kunievakateryna.data.EmailHistory;
import org.kunievakateryna.data.EmailStatus;
import org.kunievakateryna.repository.EmailHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestElasticsearchConfiguration.class)
class EmailRetrySchedulerTest {

    @Autowired
    private EmailRetryScheduler scheduler;

    @Autowired
    private EmailHistoryRepository repository;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void shouldResendEmailSuccessfully() {
        EmailHistory history = EmailHistory.builder()
                .id(UUID.randomUUID().toString())
                .recipient("retry@example.com")
                .subject("Retry")
                .content("Body")
                .status(EmailStatus.ERROR)
                .attempts(1)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(history);

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        scheduler.retryFailedEmails();

        EmailHistory updated = StreamSupport
                .stream(repository.findAll().spliterator(), false)
                .findFirst()
                .orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(updated.getAttempts()).isEqualTo(2);
        assertThat(updated.getErrorMessage()).isNull();
        assertThat(updated.getLastAttemptTime()).isNotNull();
    }

    @Test
    void shouldIncreaseAttemptsAndUpdateError_whenRetryFails() {
        EmailHistory history = EmailHistory.builder()
                .id(UUID.randomUUID().toString())
                .recipient("retry-fail@example.com")
                .subject("Retry fail")
                .content("Body")
                .status(EmailStatus.ERROR)
                .attempts(2)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(history);

        doThrow(new MailAuthenticationException("Bad credentials"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        scheduler.retryFailedEmails();

        EmailHistory updated = StreamSupport
                .stream(repository.findAll().spliterator(), false)
                .findFirst()
                .orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(EmailStatus.ERROR);
        assertThat(updated.getAttempts()).isEqualTo(3);
        assertThat(updated.getErrorMessage())
                .contains("MailAuthenticationException")
                .contains("Bad credentials");
        assertThat(updated.getLastAttemptTime()).isNotNull();
    }
}
