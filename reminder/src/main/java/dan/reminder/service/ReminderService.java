/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package dan.reminder.service;

import dan.reminder.controller.payload.CreateReminderPayload;
import dan.reminder.controller.payload.UpdateReminderPayload;
import dan.reminder.dto.ReminderDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 *
 * @author danil
 */
public interface ReminderService {

    public ReminderDto createReminder(CreateReminderPayload payload, UUID userId, String userEmail, String timezone, String idempotencyKey);

    public void deleteReminder(Long reminderId, UUID userId);

    ReminderDto updateReminder(Long reminderId, UUID userId, UpdateReminderPayload payload, String userEmail, String timezone, String idempotencyKey);

    Page<ReminderDto> findAllSortedByTitle(Pageable pageable, UUID userId, String timezone);

    Page<ReminderDto> findAllSortedByRemindDate(Pageable pageable, UUID userId, String timezone);

    Page<ReminderDto> findRemindersByTitle(String title, UUID userId, Pageable pageable, String timezone);

    Page<ReminderDto> findRemindersByDate(
            UUID userId,
            LocalDate date,
            Pageable pageable,
            String timezone
    );

    Page<ReminderDto> findRemindersByDateRange(
            UUID userId,
            Instant from,
            Instant to,
            Pageable pageable, 
            String timezone
    );

    void addSomeReminders(UUID userId, String userEmail);

}
