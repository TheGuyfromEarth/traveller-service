package com.travolish.traveller.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Emergency contact for a traveller — stored in a separate table so a user
 * can keep multiple contacts (up to MAX_CONTACTS in the controller).
 *
 * One contact per user can be marked as primary; the UI surfaces that one on
 * the account overview card and in booking/safety flows.
 */
@Entity
@Table(name = "emergency_contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK to users.id — stored as a plain Long to match SavedSearch pattern. */
    @Column(nullable = false)
    private Long userId;

    /** Full name of the contact (e.g. "Priya Gupta"). */
    @Column(nullable = false)
    private String name;

    /** Relationship to the traveller (e.g. "Spouse", "Parent", "Friend"). */
    private String relationship;

    /** Phone stored as "+91 9876543210" — same format as users.phone. */
    @Column(nullable = false)
    private String phone;

    /**
     * Only one contact per user may be primary at a time.
     * The controller enforces this by clearing existing primary before setting a new one.
     */
    @Builder.Default
    private Boolean isPrimary = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
