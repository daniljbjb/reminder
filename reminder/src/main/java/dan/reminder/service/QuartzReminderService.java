/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.service;

import dan.reminder.controller.payload.CreateReminderPayload;
import dan.reminder.controller.payload.UpdateReminderPayload;
import dan.reminder.dto.ReminderDto;
import dan.reminder.exception.IdempotencyInProgressException;
import dan.reminder.exception.InvalidDateRangeException;
import dan.reminder.exception.InvalidPageRequestException;
import dan.reminder.exception.InvalidReminderTimeException;
import dan.reminder.exception.ReminderNotFoundException;
import dan.reminder.model.IdempotencyRecord;
import dan.reminder.model.Reminder;
import dan.reminder.repository.IdempotencyRecordRepository;
import dan.reminder.repository.ReminderRepository;
import dan.reminder.scheduler.quartz.scheduler.QuartzEmailScheduler;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author danil
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuartzReminderService implements ReminderService {

    private final ReminderRepository reminderRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final QuartzEmailScheduler quartzEmailScheduler;
    private final TimezoneResolver timezoneResolver;
    private final Clock clock;

    // =============================================================
    // =============================================================
    // =============================================================
    // CREATE REMINDER METHODS
    @Override
    @Transactional
    public ReminderDto createReminder(CreateReminderPayload payload,
            UUID userId,
            String userEmail,
            String timezone,
            String idempotencyKey) {

        ZoneId zoneId = timezoneResolver.resolveRequired(timezone);
        Instant remindAt = toFutureInstant(payload.remind(), zoneId);

        acquireIdempotencyLock(userId, idempotencyKey);

        Optional<IdempotencyRecord> existingRecordOpt
                = idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);

        if (existingRecordOpt.isPresent()) {
            return getExistingReminderDto(existingRecordOpt.get(), zoneId, userId, userEmail, idempotencyKey);
        }

        IdempotencyRecord newRecord = createIdempotencyRecord(userId, idempotencyKey);
        newRecord = idempotencyRecordRepository.save(newRecord);

        Reminder savedReminder = createAndSaveReminder(payload, userId, remindAt);

        newRecord.setReminderId(savedReminder.getId());
        idempotencyRecordRepository.save(newRecord);

        quartzEmailScheduler.schedule(savedReminder, userEmail);

        log.info("Created reminder {} for user {} ({}) with idempotency key {}",
                savedReminder.getId(),
                userId,
                userEmail,
                idempotencyKey);

        return mapToDto(savedReminder, zoneId);
    }

    private void acquireIdempotencyLock(UUID userId, String idempotencyKey) {
        idempotencyRecordRepository.acquireTransactionLock(userId + ":" + idempotencyKey);
    }

    private IdempotencyRecord createIdempotencyRecord(UUID userId, String idempotencyKey) {
        return IdempotencyRecord.builder()
                .idempotencyKey(idempotencyKey)
                .userId(userId)
                .createdAt(clock.instant())
                .build();
    }

    private Reminder createAndSaveReminder(CreateReminderPayload payload,
            UUID userId,
            Instant remindAt) {

        Reminder reminder = Reminder.builder()
                .title(payload.title())
                .description(payload.description())
                .remind(remindAt)
                .userId(userId)
                .build();

        return reminderRepository.save(reminder);
    }

    private ReminderDto getExistingReminderDto(IdempotencyRecord record,
            ZoneId zoneId,
            UUID userId,
            String userEmail,
            String idempotencyKey) {

        Reminder existingReminder = getExistingReminder(record, userId, idempotencyKey);

        log.info("Returning existing reminder {} for user {} ({}) by idempotency key {}",
                existingReminder.getId(),
                userId,
                userEmail,
                idempotencyKey);

        return mapToDto(existingReminder, zoneId);
    }

    private Reminder getExistingReminder(IdempotencyRecord record,
            UUID userId,
            String idempotencyKey) {

        if (record.getReminderId() == null) {
            throw new IdempotencyInProgressException(
                    "Idempotency record exists but reminder creation is not completed yet for userId=%s, key=%s"
                            .formatted(userId, idempotencyKey)
            );
        }

        return reminderRepository.findById(record.getReminderId())
                .orElseThrow(() -> new ReminderNotFoundException(
                "Reminder not found for id=" + record.getReminderId()
        ));
    }

    // =============================================================
    // =============================================================
    // =============================================================
    @Override
    @Transactional
    public void deleteReminder(Long reminderId, UUID userId) {

        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> new ReminderNotFoundException(
                "Reminder not found for id=" + reminderId + ", userId=" + userId
        ));

        reminderRepository.delete(reminder);

        quartzEmailScheduler.delete(reminderId);

        log.info("Deleted reminder {} for user {}", reminderId, userId);
    }

    // ====================================================
    // ====================================================
    // ====================================================
    // UPDATE METHODS
    @Override
    @Transactional
    public ReminderDto updateReminder(
            Long reminderId,
            UUID userId,
            UpdateReminderPayload payload,
            String userEmail,
            String timezone,
            String idempotencyKey) {

        ZoneId zoneId = timezoneResolver.resolveRequired(timezone);
        Instant updatedRemind = payload.remind() == null
                ? null
                : toFutureInstant(payload.remind(), zoneId);

        acquireIdempotencyLock(userId, idempotencyKey);

        Optional<IdempotencyRecord> existingRecordOpt
                = idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);

        if (existingRecordOpt.isPresent()) {
            return getExistingUpdatedReminderDto(existingRecordOpt.get(), userId, zoneId, idempotencyKey);
        }

        IdempotencyRecord newRecord = createIdempotencyRecord(userId, idempotencyKey);
        newRecord = idempotencyRecordRepository.save(newRecord);

        Reminder reminder = reminderRepository.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> new ReminderNotFoundException("Reminder not found"));

        if (payload.title() != null) {
            reminder.setTitle(payload.title());
        }
        if (payload.description() != null) {
            reminder.setDescription(payload.description());
        }
        if (updatedRemind != null) {
            reminder.setRemind(updatedRemind);
        }

        Reminder savedReminder = reminderRepository.save(reminder);

        newRecord.setReminderId(savedReminder.getId());
        idempotencyRecordRepository.save(newRecord);

        quartzEmailScheduler.recreate(savedReminder, userEmail);

        log.info("Updated reminder {} for user {} with idempotency key {}",
                savedReminder.getId(),
                userId,
                idempotencyKey);

        return mapToDto(savedReminder, zoneId);
    }

    private ReminderDto getExistingUpdatedReminderDto(
            IdempotencyRecord record,
            UUID userId,
            ZoneId zoneId,
            String idempotencyKey) {

        Reminder existingReminder = getExistingUpdatedReminder(record, userId, idempotencyKey);

        log.info("Returning existing updated reminder {} for user {} by idempotency key {}",
                existingReminder.getId(),
                userId,
                idempotencyKey);

        return mapToDto(existingReminder, zoneId);
    }

    private Reminder getExistingUpdatedReminder(
            IdempotencyRecord record,
            UUID userId,
            String idempotencyKey) {

        if (record.getReminderId() == null) {
            throw new IdempotencyInProgressException(
                    "Idempotency record exists but reminder update is not completed yet for userId=%s, key=%s"
                            .formatted(userId, idempotencyKey)
            );
        }

        return reminderRepository.findByIdAndUserId(record.getReminderId(), userId)
                .orElseThrow(() -> new ReminderNotFoundException(
                "Reminder not found for existing idempotency record"
        ));
    }

    // ====================================================
    // ====================================================
    // ====================================================
    @Override
    @Transactional(readOnly = true)
    public Page<ReminderDto> findAllSortedByRemindDate(Pageable pageable, UUID userId, String timezone) {
        return findAllSortedBy(userId, pageable, "remind", timezone);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReminderDto> findAllSortedByTitle(Pageable pageable, UUID userId, String timezone) {
        return findAllSortedBy(userId, pageable, "title", timezone);
    }

    private Page<ReminderDto> findAllSortedBy(UUID userId, Pageable pageable, String field, String timezone) {

        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort().and(Sort.by("id").ascending())
                : Sort.by(Sort.Direction.DESC, field)
                        .and(Sort.by("id").ascending());

        Pageable validated;
        try {
            validated = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    sort
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidPageRequestException(pageable.getPageNumber(), pageable.getPageSize());
        }

        ZoneId zoneId = timezoneResolver.resolveOrDefault(timezone);

        return reminderRepository.findAllByUserId(userId, validated)
                .map(reminder -> mapToDto(reminder, zoneId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReminderDto> findRemindersByTitle(String title, UUID userId, Pageable pageable, String timezone) {

        Pageable validated;
        try {
            validated = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    pageable.getSort().isSorted()
                    ? pageable.getSort()
                    : Sort.by("remind").ascending().and(Sort.by("id").ascending())
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidPageRequestException(pageable.getPageNumber(), pageable.getPageSize());
        }

        ZoneId zoneId = timezoneResolver.resolveOrDefault(timezone);

        return reminderRepository.findByTitleContainingIgnoreCaseAndUserId(title, userId, validated)
                .map(reminder -> mapToDto(reminder, zoneId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReminderDto> findRemindersByDate(
            UUID userId,
            LocalDate date,
            Pageable pageable,
            String timezone) {
        ZoneId zoneId = timezoneResolver.resolveRequired(timezone);
        Instant from = date.atStartOfDay(zoneId).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(zoneId).toInstant();
        return findRemindersByDateRange(userId, from, to, pageable, timezone);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReminderDto> findRemindersByDateRange(
            UUID userId,
            Instant from,
            Instant to,
            Pageable pageable,
            String timezone
    ) {
        if (!from.isBefore(to)) {
            throw new InvalidDateRangeException(from, to);
        }

        Pageable validated;
        try {
            validated = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    pageable.getSort().isSorted()
                    ? pageable.getSort()
                    : Sort.by("remind").ascending().and(Sort.by("id").ascending())
            );
        } catch (IllegalArgumentException ex) {
            throw new InvalidPageRequestException(pageable.getPageNumber(), pageable.getPageSize());
        }

        ZoneId zoneId = timezoneResolver.resolveOrDefault(timezone);

        return reminderRepository.findByUserIdAndRemindGreaterThanEqualAndRemindLessThan(userId, from, to, validated)
                .map(reminder -> mapToDto(reminder, zoneId));
    }

    private Instant toFutureInstant(LocalDateTime localDateTime, ZoneId zoneId) {
        Instant instant = timezoneResolver.toUnambiguousInstant(localDateTime, zoneId);
        if (!instant.isAfter(clock.instant())) {
            throw new InvalidReminderTimeException(
                    "Reminder date and time must be in the future", localDateTime, zoneId);
        }
        return instant;
    }

    private ReminderDto mapToDto(Reminder reminder, ZoneId zoneId) {
        return new ReminderDto(
                reminder.getId(),
                reminder.getTitle(),
                reminder.getDescription(),
                reminder.getRemind()
                        .atZone(zoneId)
                        .toOffsetDateTime(),
                reminder.getUserId()
        );
    }

    @Transactional
    public void addSomeReminders(UUID userId, String userEmail) {

        Instant now = clock.instant();

        List<ReminderTemplate> templates = List.of(
                new ReminderTemplate("Backup files", "Make a backup of important files", 120),
                new ReminderTemplate("Call mom", "Call mom in the evening", 150),
                new ReminderTemplate("Drink water", "Drink a glass of water", 180),
                new ReminderTemplate("Exercise", "Do a short workout", 210),
                new ReminderTemplate("Fix bugs", "Fix at least one bug in project", 240),
                new ReminderTemplate("Go for a walk", "Walk outside for 15 minutes", 270),
                new ReminderTemplate("Invest time", "Study something useful", 330),
                new ReminderTemplate("Journal", "Write a short daily note", 360),
                new ReminderTemplate("Keep learning", "Watch a tech video", 390),
                new ReminderTemplate("Learn Spring", "Read about Spring features", 420),
                new ReminderTemplate("Meeting prep", "Prepare for upcoming meeting", 450),
                new ReminderTemplate("Notes review", "Review your notes", 480),
                new ReminderTemplate("Organize desk", "Clean and organize workspace", 510),
                new ReminderTemplate("Plan tomorrow", "Make a plan for next day", 540),
                new ReminderTemplate("Quick stretch", "Do a short stretch session", 570),
                new ReminderTemplate("Read book", "Read 10 pages", 600),
                new ReminderTemplate("Study Java", "Practice Java tasks", 630),
                new ReminderTemplate("Take a break", "Rest for a few minutes", 660)
        );

        List<ReminderTemplate> shuffled = new ArrayList<>(templates);
        Collections.shuffle(shuffled);

        List<Reminder> remindersToSave = shuffled.stream()
                .limit(2)
                .map(template -> Reminder.builder()
                .title(template.title())
                .description(template.description())
                .remind(now.plusSeconds(template.remindAfterSeconds()))
                .userId(userId)
                .build())
                .toList();

        List<Reminder> savedReminders = reminderRepository.saveAll(remindersToSave);

        for (Reminder savedReminder : savedReminders) {
            quartzEmailScheduler.schedule(savedReminder, userEmail);
        }

        log.info("Added {} random test reminders for user {} ({})",
                savedReminders.size(),
                userId,
                userEmail);
    }

    private record ReminderTemplate(
            String title,
            String description,
            long remindAfterSeconds
            ) {

    }
}
