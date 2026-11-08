package com.travolish.traveller.emergency.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_sos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmergencySOS {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = true)
    private Long bookingId;

    @Column(nullable = true)
    private Long hotelId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SOSStatus status;

    @Column(columnDefinition = "TEXT")
    private String emergencyDescription;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SOSType sosType;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private String userPhoneNumber;

    private String userCountry;

    private String userCity;

    @Column(nullable = false, updatable = false)
    private LocalDateTime activatedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime resolvedAt;

    private String resolutionNotes;

    private Long assignedToSupportId;

    private Integer emergencyContactsNotified;

    private Integer localAuthoritiesContacted;

    @Column(nullable = false)
    private Boolean liveLocationSharing;

    public enum SOSStatus {
        ACTIVATED,
        ACKNOWLEDGED,
        IN_PROGRESS,
        RESOLVED,
        CLOSED,
        ESCALATED
    }

    public enum SOSType {
        MEDICAL_EMERGENCY,
        SAFETY_THREAT,
        PROPERTY_DAMAGE,
        LOST_STOLEN,
        POLICE_NEEDED,
        FIRE_EMERGENCY,
        OTHER
    }

    @PrePersist
    protected void onCreate() {
        activatedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        liveLocationSharing = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
