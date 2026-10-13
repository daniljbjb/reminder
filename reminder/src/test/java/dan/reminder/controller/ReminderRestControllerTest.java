/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dan.reminder.security.SecurityConfig;
import dan.reminder.service.ReminderService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 *
 * @author danil
 */
@WebMvcTest(ReminderRestController.class)
@Import(SecurityConfig.class)
public class ReminderRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReminderService reminderService;

    @Test
    void deleteReminder_shouldReturnNoContent_whenRequestIsValid() throws Exception {
        UUID userId = UUID.randomUUID();
        Long reminderId = 42L;

        mockMvc.perform(delete("/api/v1/reminder/{id}", reminderId)
                .with(jwt().jwt(jwt -> jwt.subject(userId.toString()))))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(reminderService).deleteReminder(reminderId, userId);
    }

    @Test
    void deleteReminder_shouldReturnBadRequest_whenIdIsInvalid() throws Exception {
        mockMvc.perform(delete("/api/v1/reminder/{id}", "abc")
                .with(jwt().jwt(jwt -> jwt.subject(UUID.randomUUID().toString()))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(reminderService);
    }
}
