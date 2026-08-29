package dan.reminder.service;

import dan.reminder.controller.payload.CreateReminderPayload;
import dan.reminder.controller.payload.UpdateReminderPayload;
import dan.reminder.dto.ReminderDto;
import dan.reminder.exception.IdempotencyInProgressException;
import dan.reminder.exception.InvalidDateRangeException;
import dan.reminder.exception.InvalidReminderTimeException;
import dan.reminder.exception.InvalidTimezoneException;
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
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuartzReminderServiceTest {

    private static final ZoneId USER_ZONE = ZoneId.of("Asia/Almaty");
    private static final Instant NOW = Instant.parse("2029-01-01T00:00:00Z");

    @Mock
    private ReminderRepository reminderRepository;

    @Mock
    private IdempotencyRecordRepository idempotencyRecordRepository;

    @Mock
    private QuartzEmailScheduler quartzEmailScheduler;

    @Spy
    private TimezoneResolver timezoneResolver = new TimezoneResolver();

    @Spy
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @InjectMocks
    private QuartzReminderService reminderService;

    private UUID userId;
    private String userEmail;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userEmail = "test@mail.com";
        idempotencyKey = "key-123";
    }

    @Test
    void createReminder_shouldCreateRecordReminderAndQuartzJob_whenKeyIsNew() {
        LocalDateTime remindTime = LocalDateTime.of(2030, 5, 10, 12, 0);
        CreateReminderPayload payload = new CreateReminderPayload("Title", "Description", remindTime);

        IdempotencyRecord record = IdempotencyRecord.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .build();

        Reminder savedReminder = reminder(1L, "Title", "Description",
                remindTime.atZone(USER_ZONE).toInstant(), userId);

        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey))
                .thenReturn(Optional.empty());
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenReturn(record);
        when(reminderRepository.save(any(Reminder.class)))
                .thenReturn(savedReminder);

        ReminderDto result = reminderService.createReminder(
                payload, userId, userEmail, USER_ZONE.getId(), idempotencyKey);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.remind().toInstant()).isEqualTo(savedReminder.getRemind());
        assertThat(record.getReminderId()).isEqualTo(1L);

        InOrder order = inOrder(idempotencyRecordRepository, reminderRepository, quartzEmailScheduler);
        order.verify(idempotencyRecordRepository)
                .acquireTransactionLock(userId + ":" + idempotencyKey);
        order.verify(idempotencyRecordRepository)
                .findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        order.verify(idempotencyRecordRepository).save(any(IdempotencyRecord.class));
        order.verify(reminderRepository).save(any(Reminder.class));
        order.verify(idempotencyRecordRepository).save(record);
        order.verify(quartzEmailScheduler).schedule(savedReminder, userEmail);
    }

    @Test
    void createReminder_shouldReturnExistingReminder_withoutCreatingOrSchedulingAgain() {
        IdempotencyRecord existingRecord = IdempotencyRecord.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .reminderId(1L)
                .createdAt(Instant.now())
                .build();
        Reminder existingReminder = reminder(
                1L, "Old title", "Old description", Instant.parse("2030-01-01T10:00:00Z"), userId);

        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey))
                .thenReturn(Optional.of(existingRecord));
        when(reminderRepository.findById(1L)).thenReturn(Optional.of(existingReminder));

        ReminderDto result = reminderService.createReminder(
                new CreateReminderPayload("New title", "New description", LocalDateTime.of(2031, 1, 1, 12, 0)),
                userId, userEmail, USER_ZONE.getId(), idempotencyKey);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Old title");
        verify(idempotencyRecordRepository).acquireTransactionLock(userId + ":" + idempotencyKey);
        verify(reminderRepository, never()).save(any());
        verify(idempotencyRecordRepository, never()).save(any());
        verifyNoInteractions(quartzEmailScheduler);
    }

    @Test
    void createReminder_shouldRejectKeyWhoseFirstRequestIsStillInProgress() {
        IdempotencyRecord inProgress = IdempotencyRecord.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .build();
        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey))
                .thenReturn(Optional.of(inProgress));

        assertThatThrownBy(() -> reminderService.createReminder(
                new CreateReminderPayload("Title", "Description", LocalDateTime.of(2031, 1, 1, 12, 0)),
                userId, userEmail, USER_ZONE.getId(), idempotencyKey))
                .isInstanceOf(IdempotencyInProgressException.class);

        verifyNoInteractions(quartzEmailScheduler);
    }

    @Test
    void updateReminder_shouldChangeOnlyProvidedFields_andRecreateQuartzJob() {
        Instant originalTime = Instant.parse("2030-01-01T10:00:00Z");
        Reminder existing = reminder(42L, "Old title", "Old description", originalTime, userId);
        IdempotencyRecord record = IdempotencyRecord.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .build();

        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey))
                .thenReturn(Optional.empty());
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenReturn(record);
        when(reminderRepository.findByIdAndUserId(42L, userId)).thenReturn(Optional.of(existing));
        when(reminderRepository.save(existing)).thenReturn(existing);

        ReminderDto result = reminderService.updateReminder(
                42L, userId, new UpdateReminderPayload("New title", null, null),
                userEmail, USER_ZONE.getId(), idempotencyKey);

        assertThat(result.title()).isEqualTo("New title");
        assertThat(result.description()).isEqualTo("Old description");
        assertThat(result.remind().toInstant()).isEqualTo(originalTime);
        assertThat(record.getReminderId()).isEqualTo(42L);
        verify(quartzEmailScheduler).recreate(existing, userEmail);
    }

    @Test
    void updateReminder_shouldNotTouchReminder_whenItBelongsToAnotherUser() {
        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey))
                .thenReturn(Optional.empty());
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(reminderRepository.findByIdAndUserId(42L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.updateReminder(
                42L, userId, new UpdateReminderPayload("New title", null, null),
                userEmail, USER_ZONE.getId(), idempotencyKey))
                .isInstanceOf(ReminderNotFoundException.class);

        verify(reminderRepository, never()).save(any());
        verifyNoInteractions(quartzEmailScheduler);
    }

    @Test
    void deleteReminder_shouldDeleteReminderAndQuartzJob_whenOwnerRequests() {
        Reminder existing = reminder(
                1L, "Title", "Description", Instant.parse("2030-01-01T10:00:00Z"), userId);
        when(reminderRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.of(existing));

        reminderService.deleteReminder(1L, userId);

        verify(reminderRepository).delete(existing);
        verify(quartzEmailScheduler).delete(1L);
    }

    @Test
    void deleteReminder_shouldThrowException_withoutChangingQuartz_whenReminderIsMissing() {
        when(reminderRepository.findByIdAndUserId(1L, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reminderService.deleteReminder(1L, userId))
                .isInstanceOf(ReminderNotFoundException.class);

        verify(reminderRepository, never()).delete(any());
        verifyNoInteractions(quartzEmailScheduler);
    }

    @Test
    void findByDateRange_shouldRejectReversedRange() {
        Instant start = Instant.parse("2030-01-02T00:00:00Z");
        Instant end = Instant.parse("2030-01-01T00:00:00Z");

        assertThatThrownBy(() -> reminderService.findRemindersByDateRange(
                userId, start, end, PageRequest.of(0, 10), USER_ZONE.getId()))
                .isInstanceOf(InvalidDateRangeException.class);

        verifyNoInteractions(reminderRepository);
    }

    @Test
    void findByDate_shouldUseLocalDayInUserTimezoneAndExclusiveEnd() {
        LocalDate date = LocalDate.of(2030, 1, 2);
        PageRequest pageable = PageRequest.of(0, 10);
        PageRequest validatedPageable = PageRequest.of(
                0, 10, Sort.by("remind").ascending().and(Sort.by("id").ascending()));
        Instant expectedFrom = Instant.parse("2030-01-01T19:00:00Z");
        Instant expectedTo = Instant.parse("2030-01-02T19:00:00Z");

        when(reminderRepository.findByUserIdAndRemindGreaterThanEqualAndRemindLessThan(
                userId, expectedFrom, expectedTo, validatedPageable))
                .thenReturn(Page.empty(validatedPageable));

        reminderService.findRemindersByDate(
                userId, date, pageable, "Asia/Almaty");

        verify(reminderRepository).findByUserIdAndRemindGreaterThanEqualAndRemindLessThan(
                userId, expectedFrom, expectedTo, validatedPageable);
    }

    @Test
    void createReminder_shouldRejectUnknownTimezoneBeforeWritingData() {
        assertThatThrownBy(() -> reminderService.createReminder(
                new CreateReminderPayload("Title", "Description", LocalDateTime.of(2030, 1, 1, 12, 0)),
                userId, userEmail, "Mars/Olympus", idempotencyKey))
                .isInstanceOf(InvalidTimezoneException.class);

        verifyNoInteractions(reminderRepository, idempotencyRecordRepository, quartzEmailScheduler);
    }

    @Test
    void createReminder_shouldValidateFutureAfterConvertingUserTimezone() {
        LocalDateTime localTime = LocalDateTime.of(2029, 1, 1, 4, 0);

        assertThatThrownBy(() -> reminderService.createReminder(
                new CreateReminderPayload("Title", "Description", localTime),
                userId, userEmail, "Asia/Almaty", idempotencyKey))
                .isInstanceOf(InvalidReminderTimeException.class);

        verifyNoInteractions(reminderRepository, idempotencyRecordRepository, quartzEmailScheduler);
    }

    @Test
    void createReminder_shouldPropagateQuartzFailure() {
        IdempotencyRecord record = IdempotencyRecord.builder()
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .createdAt(Instant.now())
                .build();
        Reminder saved = reminder(
                1L, "Title", "Description", Instant.parse("2030-01-01T10:00:00Z"), userId);

        when(idempotencyRecordRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey))
                .thenReturn(Optional.empty());
        when(idempotencyRecordRepository.save(any(IdempotencyRecord.class))).thenReturn(record);
        when(reminderRepository.save(any(Reminder.class))).thenReturn(saved);
        org.mockito.Mockito.doThrow(new IllegalStateException("Quartz unavailable"))
                .when(quartzEmailScheduler).schedule(saved, userEmail);

        assertThatThrownBy(() -> reminderService.createReminder(
                new CreateReminderPayload("Title", "Description", LocalDateTime.of(2031, 1, 1, 12, 0)),
                userId, userEmail, USER_ZONE.getId(), idempotencyKey))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Quartz unavailable");
    }

    private Reminder reminder(Long id, String title, String description, Instant remind, UUID ownerId) {
        Reminder reminder = Reminder.builder()
                .title(title)
                .description(description)
                .remind(remind)
                .userId(ownerId)
                .build();
        ReflectionTestUtils.setField(reminder, "id", id);
        return reminder;
    }
}
