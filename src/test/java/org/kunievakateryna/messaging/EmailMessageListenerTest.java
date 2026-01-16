package org.kunievakateryna.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kunievakateryna.config.TestElasticsearchConfiguration;
import org.kunievakateryna.data.EmailHistory;
import org.kunievakateryna.data.EmailStatus;
import org.kunievakateryna.dto.EmailMessageDto;
import org.kunievakateryna.repository.EmailHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestElasticsearchConfiguration.class)
class EmailMessageListenerTest {

    @Autowired
    private EmailMessageListener listener;

    @Autowired
    private EmailHistoryRepository repository;

    @MockBean
    private JavaMailSender mailSender;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void shouldSaveEmailAsSent_whenSendingSuccessful() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        EmailMessageDto dto = new EmailMessageDto(
                "test@example.com",
                "Test subject",
                "Hello"
        );

        listener.receiveMessage(dto);

        List<EmailHistory> all =
                StreamSupport
                        .stream(repository.findAll().spliterator(), false)
                        .toList();

        assertThat(all).hasSize(1);

        EmailHistory history = all.get(0);
        assertThat(history.getStatus()).isEqualTo(EmailStatus.SENT);
        assertThat(history.getAttempts()).isEqualTo(1);
        assertThat(history.getErrorMessage()).isNull();
        assertThat(history.getLastAttemptTime()).isNotNull();
    }

    @Test
    void shouldSaveEmailAsError_whenSendingFails() {
        doThrow(new MailSendException("SMTP down"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        EmailMessageDto dto = new EmailMessageDto(
                "test@example.com",
                "Fail subject",
                "Fail body"
        );

        listener.receiveMessage(dto);

        EmailHistory history = StreamSupport
                .stream(repository.findAll().spliterator(), false)
                .findFirst()
                .orElseThrow();

        assertThat(history.getStatus()).isEqualTo(EmailStatus.ERROR);
        assertThat(history.getAttempts()).isEqualTo(1);
        assertThat(history.getErrorMessage())
                .contains("MailSendException")
                .contains("SMTP down");
    }
}
