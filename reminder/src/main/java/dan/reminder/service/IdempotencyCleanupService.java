package dan.reminder.service;

import dan.reminder.config.IdempotencyProperties;
import dan.reminder.repository.IdempotencyRecordRepository;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyCleanupService {

    private final IdempotencyRecordRepository repository;
    private final Clock clock;
    private final IdempotencyProperties properties;

    @Scheduled(
            initialDelayString = "${reminder.idempotency.cleanup-initial-delay:PT1M}",
            fixedDelayString = "${reminder.idempotency.cleanup-interval:PT1H}")
    @Transactional
    public int deleteExpiredRecords() {
        Instant cutoff = clock.instant().minus(properties.retention());
        int deleted = repository.deleteCreatedBefore(cutoff);
        if (deleted > 0) {
            log.info("Deleted {} expired idempotency records older than {}", deleted, cutoff);
        }
        return deleted;
    }
}
