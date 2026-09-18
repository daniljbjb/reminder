package dan.reminder.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

import dan.reminder.exception.EmailDeliveryException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class DefaultEmailServiceTest {

    @Test
    void translatesMailExceptionToDeliveryException() {
        JavaMailSender mailSender = Mockito.mock(JavaMailSender.class);
        DefaultEmailService emailService = new DefaultEmailService(mailSender);
        MailSendException mailFailure = new MailSendException("SMTP is unavailable");
        Mockito.doThrow(mailFailure).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> emailService.sendTextEmail("user@example.com", "Reminder", "Do something"))
                .isInstanceOf(EmailDeliveryException.class)
                .hasCause(mailFailure);
    }
}
