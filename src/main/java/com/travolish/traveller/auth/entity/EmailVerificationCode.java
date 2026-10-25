package com.travolish.traveller.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Short-lived one-time code emailed to a prospective user during email sign-up.
 * A new row is created each time a code is requested; verification consumes the
 * latest unconsumed row for an email.
 */
@Entity
@Table(name = "email_verification_codes", indexes = {
    @Index(name = "idx_email_verification_email", columnList = "email")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String code;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean consumed;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    // Role requested at sign-up time (GUEST / HOST) — applied to the user on verify
    @Column(name = "requested_role")
    private String requestedRole;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
