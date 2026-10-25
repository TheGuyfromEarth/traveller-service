package com.travolish.traveller.pricing.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "boost_listings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoostListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hotelId;

    @Column(nullable = false)
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoostType boostType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoostTier boostTier;

    @Column(nullable = false)
    private BigDecimal cost;

    @Column(nullable = false)
    private Integer durationDays;

    @Column(nullable = false)
    private Integer visibilityMultiplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BoostStatus status;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    private Integer impressionGain;

    private Integer clickGain;

    private Integer bookingGain;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    private String cancellationReason;

    public enum BoostType {
        FEATURED_LISTING,
        HIGHLIGHTED_SEARCH,
        PRIORITY_DISPLAY,
        PREMIUM_PROMOTION,
        SEASONAL_BOOST
    }

    public enum BoostTier {
        SILVER,
        GOLD,
        PLATINUM
    }

    public enum BoostStatus {
        PENDING_PAYMENT,
        ACTIVE,
        PAUSED,
        COMPLETED,
        CANCELLED,
        EXPIRED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
