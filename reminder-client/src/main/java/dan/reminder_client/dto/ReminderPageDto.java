/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder_client.dto;

import java.util.UUID;

/**
 *
 * @author danil
 */
public record ReminderPageDto(
        Long id,
        String title,
        String description,
        String remind,
        UUID userId
) {}
