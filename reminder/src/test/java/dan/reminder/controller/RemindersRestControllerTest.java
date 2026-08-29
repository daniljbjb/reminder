/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dan.reminder.controller.payload.CreateReminderPayload;
import dan.reminder.dto.ReminderDto;
import dan.reminder.exception.InvalidPageRequestException;
import dan.reminder.exception.ReminderNotFoundException;
import dan.reminder.model.Reminder;
import dan.reminder.security.SecurityConfig;
import dan.reminder.service.ReminderService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *
 * @author danil
 */
@WebMvcTest(RemindersRestController.class)
@Import(SecurityConfig.class)
class RemindersRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReminderService reminderService;

    @Test
    void createReminder_shouldReturnCreated_whenRequestIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "user@mail.com";
        String timezone = "Asia/Almaty";
        String idempotencyKey = UUID.randomUUID().toString();

        LocalDateTime remind = LocalDateTime.now().plusDays(10);

        CreateReminderPayload payload = new CreateReminderPayload(
                "Buy coffee",
                "Take after gym session",
                remind
        );

        ReminderDto responseDto = new ReminderDto(
                10L,
                "Buy coffee",
                "Take after gym session",
                OffsetDateTime.parse("2026-03-20T15:39:30+05:00"),
                userId
        );

        when(reminderService.createReminder(
                any(CreateReminderPayload.class),
                eq(userId),
                eq(email),
                eq(timezone),
                eq(idempotencyKey)
        )).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/reminder")
                .with(jwt().jwt(jwt -> jwt
                .subject(userId.toString())
                .claim("email", email)))
                .header("X-Timezone", timezone)
                .header("Idempotency-Key", idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/reminder/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("Buy coffee"))
                .andExpect(jsonPath("$.description").value("Take after gym session"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));

        ArgumentCaptor<CreateReminderPayload> payloadCaptor
                = ArgumentCaptor.forClass(CreateReminderPayload.class);

        verify(reminderService).createReminder(
                payloadCaptor.capture(),
                eq(userId),
                eq(email),
                eq(timezone),
                eq(idempotencyKey)
        );

        CreateReminderPayload capturedPayload = payloadCaptor.getValue();

        assertThat(capturedPayload.title()).isEqualTo("Buy coffee");
        assertThat(capturedPayload.description()).isEqualTo("Take after gym session");
        assertThat(capturedPayload.remind()).isEqualTo(remind);
    }

    @Test
    void createReminder_shouldReturnUnauthorized_whenJwtIsMissing() throws Exception {

        LocalDateTime remind = LocalDateTime.now().plusDays(10);

        CreateReminderPayload payload = new CreateReminderPayload(
                "Buy coffee",
                "Take after gym session",
                remind
        );

        mockMvc.perform(post("/api/v1/reminder")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(reminderService);
    }

    @Test
    void createReminder_shouldReturnBadRequest_whenIdempotencyKeyIsMissing() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateReminderPayload payload = new CreateReminderPayload(
                "Buy coffee", "Take after gym session", LocalDateTime.now().plusDays(10));

        mockMvc.perform(post("/api/v1/reminder")
                .with(jwt().jwt(token -> token
                .subject(userId.toString())
                .claim("email", "user@mail.com")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reminderService);
    }

    @Test
    void createReminder_shouldReturnBadRequest_whenIdempotencyKeyIsBlank() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateReminderPayload payload = new CreateReminderPayload(
                "Buy coffee", "Take after gym session", LocalDateTime.now().plusDays(10));

        mockMvc.perform(post("/api/v1/reminder")
                .with(jwt().jwt(token -> token
                .subject(userId.toString())
                .claim("email", "user@mail.com")))
                .header("Idempotency-Key", " ")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(reminderService);
    }

    @Test
    void createReminder_shouldReturnValidationDetails_whenPayloadIsInvalid() throws Exception {
        UUID userId = UUID.randomUUID();
        CreateReminderPayload payload = new CreateReminderPayload(
                "", "x", LocalDateTime.now().minusDays(1));

        mockMvc.perform(post("/api/v1/reminder")
                .with(jwt().jwt(token -> token
                .subject(userId.toString())
                .claim("email", "user@mail.com")))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details.title").isArray())
                .andExpect(jsonPath("$.details.description").isArray());

        verifyNoInteractions(reminderService);
    }

    @Test
    void findAllSortedByTitle_shouldReturnPage_whenDefaultPageableIsUsed() throws Exception {
        UUID userId = UUID.randomUUID();
        String timezone = "Asia/Almaty";

        ReminderDto reminder = new ReminderDto(
                1L,
                "Buy coffee",
                "Take after gym session",
                OffsetDateTime.now().plusDays(1),
                userId
        );

        Page<ReminderDto> responsePage = new PageImpl<>(
                List.of(reminder),
                PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "title")),
                1
        );

        when(reminderService.findAllSortedByTitle(
                any(Pageable.class),
                eq(userId),
                eq(timezone)
        )).thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/reminder/sort/title")
                .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                .header("X-Timezone", timezone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Buy coffee"))
                .andExpect(jsonPath("$.content[0].description").value("Take after gym session"))
                .andExpect(jsonPath("$.content[0].userId").value(userId.toString()))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        verify(reminderService).findAllSortedByTitle(
                pageableCaptor.capture(),
                eq(userId),
                eq(timezone)
        );

        Pageable capturedPageable = pageableCaptor.getValue();

        assertThat(capturedPageable.getPageNumber()).isEqualTo(0);
        assertThat(capturedPageable.getPageSize()).isEqualTo(3);
        assertThat(capturedPageable.getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "title"));
    }

    @Test
    void findAllSortedByTitle_shouldReturnBadRequest_whenPageRequestIsInvalid() throws Exception {
        UUID userId = UUID.randomUUID();
        String timezone = "Asia/Almaty";

        InvalidPageRequestException exception
                = new InvalidPageRequestException(0, -1);

        when(reminderService.findAllSortedByTitle(
                any(Pageable.class),
                eq(userId),
                eq(timezone)
        )).thenThrow(exception);

        mockMvc.perform(get("/api/v1/reminder/sort/title")
                .param("page", "0")
                .param("size", "-1")
                .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                .header("X-Timezone", timezone))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(exception.getErrorCode()))
                .andExpect(jsonPath("$.message").value(exception.getMessage()))
                .andExpect(jsonPath("$.details.page").value(0))
                .andExpect(jsonPath("$.details.size").value(-1));

        verify(reminderService).findAllSortedByTitle(
                any(Pageable.class),
                eq(userId),
                eq(timezone)
        );
    }

    @Test
    void findAllSortedByDate_shouldUseReminderTimeByDefault() throws Exception {
        UUID userId = UUID.randomUUID();
        Page<ReminderDto> responsePage = new PageImpl<>(List.of());

        when(reminderService.findAllSortedByRemindDate(any(Pageable.class), eq(userId), any()))
                .thenReturn(responsePage);

        mockMvc.perform(get("/api/v1/reminder/sort/date")
                .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                .header("X-Timezone", "UTC"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reminderService).findAllSortedByRemindDate(
                pageableCaptor.capture(), eq(userId), any());

        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "remind"));
    }

    @Test
    void findAllSortedByTitle_shouldReturnUnifiedNotFoundError() throws Exception {
        UUID userId = UUID.randomUUID();
        when(reminderService.findAllSortedByTitle(any(Pageable.class), eq(userId), any()))
                .thenThrow(new ReminderNotFoundException("not exposed"));

        mockMvc.perform(get("/api/v1/reminder/sort/title")
                .with(jwt().jwt(jwt -> jwt.subject(userId.toString())))
                .header("X-Timezone", "UTC"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("REMINDER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Reminder not found"))
                .andExpect(jsonPath("$.details").isMap());
    }

}
