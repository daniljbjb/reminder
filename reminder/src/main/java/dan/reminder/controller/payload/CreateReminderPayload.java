/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.controller.payload;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 *
 * @author danil
 */
public record CreateReminderPayload(
        @NotBlank(message = "{reminders.create.errors.title_is_blank}")
        @Size(min = 3, max = 255, message = "{reminders.create.errors.title_size_is_invalid}")
        String title,
        @NotBlank(message = "{reminders.create.errors.description_is_blank}")
        @Size(min = 3, max = 4096, message = "{reminders.create.errors.description_size_is_invalid}")
        String description,
        @NotNull(message = "{reminders.create.errors.instant_is_null}")
        LocalDateTime remind
        ) {

}
