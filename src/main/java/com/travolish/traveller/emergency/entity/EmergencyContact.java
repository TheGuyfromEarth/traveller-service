package com.travolish.traveller.emergency.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

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
