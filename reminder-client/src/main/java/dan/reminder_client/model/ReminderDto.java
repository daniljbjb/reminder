/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 *
 * @author danil
 */
public record ReminderDto(
        Long id,
        String title,
        String description,
        OffsetDateTime remind,
        UUID userId
        ) {

}
