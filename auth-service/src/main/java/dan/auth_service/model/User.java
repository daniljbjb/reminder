/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dan.auth_service.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 *
 * @author danil
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "users",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_users_public_id", columnNames = "public_id"),
            @UniqueConstraint(name = "uk_users_provider_provider_id", columnNames = {"provider", "provider_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User {

    @Builder
    public User(OAuthProvider provider, String providerId, String email, Set<Role> roles) {
        this.publicId = UUID.randomUUID();
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        if (roles != null) {
            this.roles.addAll(roles);
        }
    }

    // INTERNAL DB ID
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
    @SequenceGenerator(
            name = "user_seq",
            sequenceName = "user_seq",
            allocationSize = 15
    )
    @Column(name = "id", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Long id;

    // STABLE PUBLIC ID (лучше всего класть в JWT как sub)
    @Column(name = "public_id", nullable = false, updatable = false, unique = true)
    @Setter(AccessLevel.NONE)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 32, updatable = false)
    @Setter(AccessLevel.NONE)
    private OAuthProvider provider;

    // Например, Google "sub"
    @Size(max = 255)
    @Column(name = "provider_id", nullable = false, length = 255, updatable = false)
    @Setter(AccessLevel.NONE)
    private String providerId;

    @Size(max = 255)
    @Column(name = "email", nullable = false, length = 255)
    @Setter(AccessLevel.NONE)
    private String email;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    @Setter(AccessLevel.NONE)
    private Set<Role> roles = new HashSet<>();

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    private void normalize() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }
        if (providerId != null) {
            providerId = providerId.trim();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (org.hibernate.Hibernate.getClass(this) != org.hibernate.Hibernate.getClass(o)) {
            return false;
        }
        User other = (User) o;
        return publicId != null && publicId.equals(other.publicId);
    }

    @Override
    public int hashCode() {
        return (publicId != null) ? publicId.hashCode() : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "User{"
                + "id=" + id
                + ", publicId=" + publicId
                + ", provider=" + provider
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }

}
