/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dan.reminder.controller.payload.CreateReminderPayload;
import dan.reminder.dto.ReminderDto;
import dan.reminder.model.Reminder;
import dan.reminder.repository.IdempotencyRecordRepository;
import dan.reminder.repository.ReminderRepository;
import dan.reminder.scheduler.quartz.scheduler.QuartzEmailScheduler;
import dan.reminder.service.ReminderService;
import java.time.LocalDateTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
class RemindersRestControllerIT {

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
    void createReminder_shouldCreateReminderInDatabase_whenRequestIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "user@mail.com";
        String timezone = "Asia/Almaty";
        String idempotencyKey = UUID.randomUUID().toString();

        CreateReminderPayload payload = new CreateReminderPayload(
                "Buy coffee",
                "Take after gym session",
                LocalDateTime.now().plusDays(10)
        );

        mockMvc.perform(post("/api/v1/reminder")
                .with(jwt().jwt(jwt -> jwt
                .subject(userId.toString())
                .claim("email", email)))
                .header("X-Timezone", timezone)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Buy coffee"))
                .andExpect(jsonPath("$.description").value("Take after gym session"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));

        List<Reminder> reminders = reminderRepository.findAll();

        assertThat(reminders).hasSize(1);

        Reminder savedReminder = reminders.get(0);

        assertThat(savedReminder.getTitle()).isEqualTo("Buy coffee");
        assertThat(savedReminder.getDescription()).isEqualTo("Take after gym session");
        assertThat(savedReminder.getUserId()).isEqualTo(userId);

        assertThat(idempotencyRecordRepository.findAll()).hasSize(1);

        verify(quartzEmailScheduler).schedule(any(Reminder.class), eq(email));
    }

    @Test
    void createReminder_shouldReturnUnauthorized_whenUserIsNotAuthenticated() throws Exception {
        String timezone = "Asia/Almaty";
        String idempotencyKey = UUID.randomUUID().toString();

        CreateReminderPayload payload = new CreateReminderPayload(
                "Buy coffee",
                "Take after gym session",
                LocalDateTime.now().plusDays(10)
        );

        mockMvc.perform(post("/api/v1/reminder")
                .header("X-Timezone", timezone)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        assertThat(reminderRepository.findAll()).isEmpty();
        assertThat(idempotencyRecordRepository.findAll()).isEmpty();

        verifyNoInteractions(quartzEmailScheduler);
    }

    @Test
    void findAllSortedByTitle_shouldReturnFirstPageSortedByTitleDesc_whenDefaultPageableIsUsed() throws Exception {
        UUID userId = UUID.randomUUID();

        saveReminder(userId, "Alpha");
        saveReminder(userId, "Bravo");
        saveReminder(userId, "Charlie");
        saveReminder(userId, "Delta");

        mockMvc.perform(get("/api/v1/reminder/sort/title")
                .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                .header("X-Timezone", "Asia/Almaty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Delta"))
                .andExpect(jsonPath("$.content[1].title").value("Charlie"))
                .andExpect(jsonPath("$.content[2].title").value("Bravo"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void findAllSortedByTitle_shouldUseRequestPageSizeAndSort_whenPageableParamsAreProvided() throws Exception {
        UUID userId = UUID.randomUUID();

        saveReminder(userId, "Delta");
        saveReminder(userId, "Charlie");
        saveReminder(userId, "Bravo");
        saveReminder(userId, "Alpha");

        mockMvc.perform(get("/api/v1/reminder/sort/title")
                .param("page", "0")
                .param("size", "2")
                .param("sort", "title,asc")
                .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                .header("X-Timezone", "Asia/Almaty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].title").value("Alpha"))
                .andExpect(jsonPath("$.content[1].title").value("Bravo"))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void createReminder_shouldReturnExistingResult_whenRequestIsRepeatedWithSameKey() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "user@mail.com";
        String idempotencyKey = UUID.randomUUID().toString();
        CreateReminderPayload payload = new CreateReminderPayload(
                "Buy coffee", "Take after gym session", LocalDateTime.now().plusDays(10));

        String firstResponse = mockMvc.perform(post("/api/v1/reminder")
                .with(jwt().jwt(token -> token.subject(userId.toString()).claim("email", email)))
                .header("X-Timezone", "Asia/Almaty")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        ReminderDto first = objectMapper.readValue(firstResponse, ReminderDto.class);

        mockMvc.perform(post("/api/v1/reminder")
                .with(jwt().jwt(token -> token.subject(userId.toString()).claim("email", email)))
                .header("X-Timezone", "Asia/Almaty")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(first.id()));

        assertThat(reminderRepository.findAll()).hasSize(1);
        assertThat(idempotencyRecordRepository.findAll()).hasSize(1);
        verify(quartzEmailScheduler, times(1)).schedule(any(Reminder.class), eq(email));
    }

    @Test
    void createReminder_shouldRollbackDatabase_whenQuartzSchedulingFails() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "user@mail.com";
        doThrow(new IllegalStateException("Quartz unavailable"))
                .when(quartzEmailScheduler).schedule(any(Reminder.class), eq(email));

        CreateReminderPayload payload = new CreateReminderPayload(
                "Buy coffee", "Take after gym session", LocalDateTime.now().plusDays(10));

        mockMvc.perform(post("/api/v1/reminder")
                .with(jwt().jwt(token -> token.subject(userId.toString()).claim("email", email)))
                .header("X-Timezone", "Asia/Almaty")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isInternalServerError());

        assertThat(reminderRepository.findAll()).isEmpty();
        assertThat(idempotencyRecordRepository.findAll()).isEmpty();
    }

    @Test
    void findAllSortedByTitle_shouldReturnOnlyCurrentUserReminders() throws Exception {
        UUID currentUser = UUID.randomUUID();
        UUID anotherUser = UUID.randomUUID();
        saveReminder(currentUser, "Visible");
        saveReminder(anotherUser, "Hidden");

        mockMvc.perform(get("/api/v1/reminder/sort/title")
                .with(jwt().jwt(token -> token.subject(currentUser.toString())))
                .header("X-Timezone", "Asia/Almaty"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Visible"))
                .andExpect(jsonPath("$.content[0].userId").value(currentUser.toString()));
    }

    private Reminder saveReminder(UUID userId, String title) {
        return reminderRepository.save(
                Reminder.builder()
                        .title(title)
                        .description("Test description")
                        .remind(Instant.parse("2030-01-01T10:00:00Z"))
                        .userId(userId)
                        .build()
        );
    }

}
