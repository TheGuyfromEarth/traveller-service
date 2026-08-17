package com.travolish.traveller.emergency.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Hotel-attached and city-level public emergency services (police, ambulance, hospital, etc.).
 *
 * <p>The JPA entity name is {@code EmergencyService} (not the default {@code EmergencyContact})
 * to avoid a Hibernate entity-name collision with
 * {@code com.travolish.traveller.user.entity.EmergencyContact}, which stores a traveller's
 * personal ICE contacts.  The table is {@code emergency_services} for the same reason —
 * both entities previously mapped to {@code emergency_contacts}, which is now the user module's
 * table exclusively.
 */
@Entity(name = "EmergencyService")
@Table(name = "emergency_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long hotelId;

    private String label;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContactType contactType;

    @Column(nullable = false)
    private String contactNumber;

    @Column(nullable = false)
    private String contactName;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String address;

    private Double latitude;

    private Double longitude;

    private String email;

    private String operatingHours;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime verifiedAt;

    private Integer responseTimeMinutes;

    public enum ContactType {
        POLICE,
        AMBULANCE,
        FIRE_DEPARTMENT,
        HOSPITAL,
        EMBASSY,
        CONSULATE,
        TOURIST_POLICE,
        LOCAL_AUTHORITY,
        NGO,
        OTHER
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
