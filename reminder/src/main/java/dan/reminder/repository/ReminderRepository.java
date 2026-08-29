/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package dan.reminder.repository;

import dan.reminder.model.Reminder;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author danil
 */
@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    Optional<Reminder> findByIdAndUserId(Long reminderId, UUID userId);

    Page<Reminder> findAllByUserId(UUID userId, Pageable pageable);

    // using gin index
    Page<Reminder> findByTitleContainingIgnoreCaseAndUserId(
            String title,
            UUID userId,
            Pageable pageable
    );

    Page<Reminder> findByUserIdAndRemindGreaterThanEqualAndRemindLessThan(
            UUID userId,
            Instant from,
            Instant to,
            Pageable pageable
    );

}
