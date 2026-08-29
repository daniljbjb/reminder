/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.reminder.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

/**
 *
 * @author danil
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reminders")
@Access(AccessType.FIELD)
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "reminder_seq")
    @SequenceGenerator(
            name = "reminder_seq",
            sequenceName = "reminder_seq",
            allocationSize = 5
    )
    @Setter(AccessLevel.NONE)
    private Long id;

    @Setter
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Setter
    @Column(name = "description", nullable = false, length = 4096)
    private String description;

    @Setter
    @Column(name = "remind", nullable = false)
    private Instant remind;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Builder
    private Reminder(String title, String description, Instant remind, UUID userId) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.remind = Objects.requireNonNull(remind, "remind must not be null");
        this.userId = Objects.requireNonNull(userId, "userId must not be null");
    }

    public void updateContent(String title, String description, Instant remind) {
        this.title = Objects.requireNonNull(title, "title must not be null");
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.remind = Objects.requireNonNull(remind, "remind must not be null");
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        Reminder other = (Reminder) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public final int hashCode() {
        return (id != null) ? id.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "Reminder{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", remind=" + remind +
                ", userId=" + userId +
                '}';
    }
}
