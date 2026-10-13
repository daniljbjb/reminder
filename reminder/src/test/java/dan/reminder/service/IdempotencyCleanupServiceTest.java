package dan.reminder.service;

import dan.reminder.config.IdempotencyProperties;
import dan.reminder.repository.IdempotencyRecordRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdempotencyCleanupServiceTest {

    private static final Instant NOW = Instant.parse("2030-01-02T12:00:00Z");

    @Test
    void deleteExpiredRecords_shouldDeleteRecordsOlderThanRetention() {
        IdempotencyRecordRepository repository = mock(IdempotencyRecordRepository.class);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        Instant cutoff = Instant.parse("2030-01-01T12:00:00Z");
        when(repository.deleteCreatedBefore(cutoff)).thenReturn(3);
        IdempotencyCleanupService service = new IdempotencyCleanupService(
                repository,
                clock,
                new IdempotencyProperties(
                        Duration.ofHours(24), Duration.ofMinutes(1), Duration.ofHours(1)));

        int deleted = service.deleteExpiredRecords();

        assertThat(deleted).isEqualTo(3);
        verify(repository).deleteCreatedBefore(cutoff);
    }

    @Test
    void properties_shouldRejectNonPositiveRetention() {
        assertThatThrownBy(() -> new IdempotencyProperties(
                Duration.ZERO, Duration.ofMinutes(1), Duration.ofHours(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
