package dan.reminder.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import dan.reminder.model.Reminder;
import dan.reminder.repository.IdempotencyRecordRepository;
import dan.reminder.repository.ReminderRepository;
import dan.reminder.scheduler.quartz.scheduler.QuartzEmailScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
    "spring.mail.host=127.0.0.1",
    "spring.mail.port=3025",
    "spring.mail.username=",
    "spring.mail.password=",
    "spring.mail.properties.mail.smtp.auth=false",
    "spring.mail.properties.mail.smtp.starttls.enable=false",
    "reminder.email.retry.max-attempts=2",
    "reminder.email.retry.initial-delay=PT1S"
})
@AutoConfigureMockMvc
@Testcontainers
class QuartzEmailRetryIT {

    private static final GreenMail SMTP_SERVER = new GreenMail(
            new ServerSetup(3025, "127.0.0.1", ServerSetup.PROTOCOL_SMTP));

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    Scheduler scheduler;

    @Autowired
    ReminderRepository reminderRepository;

    @Autowired
    IdempotencyRecordRepository idempotencyRecordRepository;

    @Autowired
    QuartzEmailScheduler quartzEmailScheduler;

    @BeforeEach
    void cleanState() throws Exception {
        if (SMTP_SERVER.isRunning()) {
            SMTP_SERVER.stop();
        }
        scheduler.clear();
        idempotencyRecordRepository.deleteAll();
        reminderRepository.deleteAll();
    }

    @AfterEach
    void stopSmtpServer() {
        if (SMTP_SERVER.isRunning()) {
            SMTP_SERVER.stop();
        }
    }

    @Test
    void healthProbesArePublicAndRetryDeliversThroughSmtpAfterServerRecovers() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        Reminder reminder = reminderRepository.saveAndFlush(Reminder.builder()
                .title("Retry integration test")
                .description("Quartz must persist the retry trigger")
                .remind(Instant.now().plusSeconds(1))
                .userId(UUID.randomUUID())
                .build());
        JobKey jobKey = JobKey.jobKey("email-job-" + reminder.getId(), "email-jobs");

        quartzEmailScheduler.schedule(reminder, "user@example.com");

        assertThat(waitUntil(Duration.ofSeconds(10), () -> hasRetryTrigger(jobKey))).isTrue();

        SMTP_SERVER.start();

        assertThat(SMTP_SERVER.waitForIncomingEmail(10_000, 1)).isTrue();
        MimeMessage message = SMTP_SERVER.getReceivedMessages()[0];
        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("user@example.com");
        assertThat(message.getSubject()).isEqualTo("Retry integration test");
    }

    private boolean waitUntil(Duration timeout, BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            Thread.sleep(50);
        }
        return condition.getAsBoolean();
    }

    private boolean hasRetryTrigger(JobKey jobKey) {
        try {
            return scheduler.getTriggersOfJob(jobKey).stream()
                    .anyMatch(trigger -> trigger.getKey().getName().contains("-retry-2"));
        } catch (org.quartz.SchedulerException ex) {
            throw new IllegalStateException("Unable to inspect Quartz retry trigger", ex);
        }
    }
}
