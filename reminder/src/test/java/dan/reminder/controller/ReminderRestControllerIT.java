/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dan.reminder.controller.payload.UpdateReminderPayload;
import dan.reminder.model.Reminder;
import dan.reminder.repository.IdempotencyRecordRepository;
import dan.reminder.repository.ReminderRepository;
import dan.reminder.scheduler.quartz.scheduler.QuartzEmailScheduler;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 *
 * @author danil
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class ReminderRestControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres
            = new PostgreSQLContainer<>("postgres:16");

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
    ReminderRepository reminderRepository;

    @Autowired
    IdempotencyRecordRepository idempotencyRecordRepository;

    @MockBean
    QuartzEmailScheduler quartzEmailScheduler;

    @BeforeEach
    void cleanDb() {
        idempotencyRecordRepository.deleteAll();
        reminderRepository.deleteAll();
    }

    @Test
    void deleteReminder_shouldDeleteReminder_whenOwnerRequests() throws Exception {
        UUID userId = UUID.randomUUID();

        Reminder reminder = reminderRepository.save(
                Reminder.builder()
                        .title("Test")
                        .description("Test desc")
                        .remind(Instant.parse("2030-01-01T10:00:00Z"))
                        .userId(userId)
                        .build()
        );

        mockMvc.perform(delete("/api/v1/reminder/{id}", reminder.getId())
                .with(jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNoContent());

        assertThat(reminderRepository.findById(reminder.getId())).isEmpty();
    }

    @Test
    void deleteReminder_shouldReturnNotFound_whenReminderDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        Long nonExistingId = 999999L;

        mockMvc.perform(delete("/api/v1/reminder/{id}", nonExistingId)
                .with(jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNotFound());

        assertThat(reminderRepository.findAll()).isEmpty();
    }

    @Test
    void updateReminder_shouldUpdateReminder_whenOwnerRequests() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "user@mail.com";

        Reminder reminder = reminderRepository.save(
                Reminder.builder()
                        .title("Old title")
                        .description("Old desc")
                        .remind(Instant.parse("2030-01-01T10:00:00Z"))
                        .userId(userId)
                        .build()
        );

        UpdateReminderPayload payload = new UpdateReminderPayload(
                "New title",
                "New desc",
                LocalDateTime.of(2031, 1, 1, 12, 0)
        );

        mockMvc.perform(patch("/api/v1/reminder/{id}", reminder.getId())
                .with(jwt().jwt(jwt -> jwt
                .subject(userId.toString())
                .claim("email", email)))
                .header("X-Timezone", "Asia/Almaty")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reminder.getId()))
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("New desc"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));

        Reminder updated = reminderRepository.findById(reminder.getId()).orElseThrow();

        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(updated.getDescription()).isEqualTo("New desc");
    }

    @Test
    void updateReminder_shouldReturnNotFound_whenUserIsNotOwner() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID anotherUserId = UUID.randomUUID();
        String email = "user@mail.com";

        Reminder reminder = reminderRepository.save(
                Reminder.builder()
                        .title("Old title")
                        .description("Old desc")
                        .remind(Instant.parse("2030-01-01T10:00:00Z"))
                        .userId(ownerId)
                        .build()
        );

        UpdateReminderPayload payload = new UpdateReminderPayload(
                "New title",
                "New desc",
                LocalDateTime.of(2031, 1, 1, 12, 0)
        );

        mockMvc.perform(patch("/api/v1/reminder/{id}", reminder.getId())
                .with(jwt().jwt(jwt -> jwt
                .subject(anotherUserId.toString())
                .claim("email", email)))
                .header("X-Timezone", "Asia/Almaty")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());

        Reminder notUpdated = reminderRepository.findById(reminder.getId()).orElseThrow();

        assertThat(notUpdated.getTitle()).isEqualTo("Old title");
        assertThat(notUpdated.getDescription()).isEqualTo("Old desc");
    }

    @Test
    void updateReminder_shouldPreserveOmittedFields_whenPatchIsPartial() throws Exception {
        UUID userId = UUID.randomUUID();
        Instant originalRemind = Instant.parse("2030-01-01T10:00:00Z");
        Reminder reminder = reminderRepository.save(
                Reminder.builder()
                        .title("Old title")
                        .description("Old description")
                        .remind(originalRemind)
                        .userId(userId)
                        .build());

        UpdateReminderPayload payload = new UpdateReminderPayload("New title", null, null);

        mockMvc.perform(patch("/api/v1/reminder/{id}", reminder.getId())
                .with(jwt().jwt(token -> token
                .subject(userId.toString())
                .claim("email", "user@mail.com")))
                .header("X-Timezone", "Asia/Almaty")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("Old description"));

        Reminder updated = reminderRepository.findById(reminder.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(updated.getDescription()).isEqualTo("Old description");
        assertThat(updated.getRemind()).isEqualTo(originalRemind);
    }

    @Test
    void updateReminder_shouldReturnBadRequest_whenIdempotencyKeyIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        Reminder reminder = reminderRepository.save(
                Reminder.builder()
                        .title("Old title")
                        .description("Old description")
                        .remind(Instant.parse("2030-01-01T10:00:00Z"))
                        .userId(userId)
                        .build());

        mockMvc.perform(patch("/api/v1/reminder/{id}", reminder.getId())
                .with(jwt().jwt(token -> token
                .subject(userId.toString())
                .claim("email", "user@mail.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"New title\"}"))
                .andExpect(status().isBadRequest());

        assertThat(reminderRepository.findById(reminder.getId()).orElseThrow().getTitle())
                .isEqualTo("Old title");
    }
}
