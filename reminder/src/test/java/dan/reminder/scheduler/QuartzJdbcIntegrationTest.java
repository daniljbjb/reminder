package dan.reminder.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dan.reminder.controller.payload.CreateReminderPayload;
import dan.reminder.dto.ReminderDto;
import dan.reminder.repository.IdempotencyRecordRepository;
import dan.reminder.repository.ReminderRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.quartz.auto-startup=false")
@AutoConfigureMockMvc
@Testcontainers
class QuartzJdbcIntegrationTest {

    private static final String JOB_GROUP = "email-jobs";

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
    ObjectMapper objectMapper;

    @Autowired
    Scheduler scheduler;

    @Autowired
    ReminderRepository reminderRepository;

    @Autowired
    IdempotencyRecordRepository idempotencyRecordRepository;

    @BeforeEach
    void cleanState() throws Exception {
        scheduler.clear();
        idempotencyRecordRepository.deleteAll();
        reminderRepository.deleteAll();
    }

    @Test
    void createAndDeleteReminder_shouldUpdateBusinessAndQuartzTablesTogether() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "user@mail.com";
        CreateReminderPayload payload = new CreateReminderPayload(
                "Integration reminder",
                "Quartz JDBC test",
                LocalDateTime.now().plusDays(10));

        String response = mockMvc.perform(post("/api/v1/reminder")
                .with(jwt().jwt(token -> token.subject(userId.toString()).claim("email", email)))
                .header("X-Timezone", "Asia/Almaty")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ReminderDto created = objectMapper.readValue(response, ReminderDto.class);
        JobKey jobKey = JobKey.jobKey("email-job-" + created.id(), JOB_GROUP);
        TriggerKey triggerKey = TriggerKey.triggerKey("email-trigger-" + created.id(), JOB_GROUP);

        assertThat(reminderRepository.findById(created.id())).isPresent();
        assertThat(scheduler.checkExists(jobKey)).isTrue();
        assertThat(scheduler.checkExists(triggerKey)).isTrue();

        mockMvc.perform(delete("/api/v1/reminder/{id}", created.id())
                .with(jwt().jwt(token -> token.subject(userId.toString()))))
                .andExpect(status().isNoContent());

        assertThat(reminderRepository.findById(created.id())).isEmpty();
        assertThat(scheduler.checkExists(jobKey)).isFalse();
        assertThat(scheduler.checkExists(triggerKey)).isFalse();
    }
}
