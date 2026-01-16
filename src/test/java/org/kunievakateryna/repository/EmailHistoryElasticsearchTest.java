package org.kunievakateryna.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kunievakateryna.config.TestElasticsearchConfiguration;
import org.kunievakateryna.data.EmailHistory;
import org.kunievakateryna.data.EmailStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestElasticsearchConfiguration.class)
class EmailHistoryElasticsearchTest {

    @Autowired
    private EmailHistoryRepository repository;

    @BeforeEach
    void cleanIndex() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveAndFindByStatus() {
        EmailHistory pending = EmailHistory.builder()
                .id(UUID.randomUUID().toString())
                .recipient("pending@test.com")
                .subject("Pending email")
                .content("Body")
                .status(EmailStatus.PENDING)
                .attempts(0)
                .createdAt(LocalDateTime.now())
                .build();

        EmailHistory error = EmailHistory.builder()
                .id(UUID.randomUUID().toString())
                .recipient("error@test.com")
                .subject("Error email")
                .content("Body")
                .status(EmailStatus.ERROR)
                .attempts(1)
                .errorMessage("SMTP down")
                .createdAt(LocalDateTime.now())
                .lastAttemptTime(LocalDateTime.now())
                .build();

        repository.save(pending);
        repository.save(error);

        List<EmailHistory> errorEmails =
                repository.findByStatus(EmailStatus.ERROR);

        assertThat(errorEmails).hasSize(1);

        EmailHistory loaded = errorEmails.get(0);
        assertThat(loaded.getRecipient()).isEqualTo("error@test.com");
        assertThat(loaded.getStatus()).isEqualTo(EmailStatus.ERROR);
        assertThat(loaded.getAttempts()).isEqualTo(1);
        assertThat(loaded.getErrorMessage()).isEqualTo("SMTP down");
        assertThat(loaded.getLastAttemptTime()).isNotNull();
    }

    @Test
    void shouldUpdateExistingDocument() {
        EmailHistory history = EmailHistory.builder()
                .id(UUID.randomUUID().toString())
                .recipient("update@test.com")
                .subject("Update test")
                .content("Body")
                .status(EmailStatus.ERROR)
                .attempts(1)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(history);

        history.setStatus(EmailStatus.SENT);
        history.setAttempts(2);
        history.setErrorMessage(null);
        history.setLastAttemptTime(LocalDateTime.now());

        repository.save(history);

        EmailHistory updated =
                repository.findById(history.getId()).orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(updated.getAttempts()).isEqualTo(2);
        assertThat(updated.getErrorMessage()).isNull();
        assertThat(updated.getLastAttemptTime()).isNotNull();
    }
}
