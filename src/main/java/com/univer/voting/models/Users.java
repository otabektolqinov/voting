package com.univer.voting.models;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import com.univer.voting.enums.UserRole;
import com.univer.voting.enums.RegistrationType;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_national_id", columnList = "national_id"),
        @Index(name = "idx_users_role", columnList = "role")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "national_id", unique = true, nullable = false, length = 50)
    private String nationalId;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;

    @Column(name = "verification_token")
    private UUID verificationToken;

    @Column(name = "activation_token")
    private UUID activationToken;

    @Column(name = "account_activated", nullable = false)
    @Builder.Default
    private Boolean accountActivated = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private UserRole role = UserRole.VOTER;

    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "imported_by")
    private UUID importedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", length = 20, nullable = false)
    @Builder.Default
    private RegistrationType registrationType = RegistrationType.SELF;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean canVote() {
        return accountActivated && emailVerified && !accountLocked;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isElectionOfficer() {
        return role == UserRole.ELECTION_OFFICER;
    }


    public void incrementFailedLoginAttempts(int maxAttempts) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.accountLocked = true;
            this.lockedUntil = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(60);
        }
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lastLoginAt = LocalDateTime.now();
    }

    public boolean needsActivation() {
        return !accountActivated && activationToken != null;
    }

    public boolean isLockExpired() {
        return lockedUntil != null && LocalDateTime.now(ZoneOffset.UTC).isAfter(lockedUntil);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", accountActivated=" + accountActivated +
                ", emailVerified=" + emailVerified +
                '}';
    }
}
