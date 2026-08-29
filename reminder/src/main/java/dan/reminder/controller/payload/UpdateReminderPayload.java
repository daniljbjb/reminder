/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.controller.payload;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 *
 * @author danil
 */
public record UpdateReminderPayload(
        @Nullable
        @Size(min = 3, max = 255, message = "{reminders.update.errors.title_size_is_invalid}")
        String title,
        @Nullable
        @Size(min = 3, max = 4096, message = "{reminders.update.errors.description_size_is_invalid}")
        String description,
        @Nullable
        LocalDateTime remind
        ) {

}
