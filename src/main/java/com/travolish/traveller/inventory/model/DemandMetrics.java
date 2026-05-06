package com.travolish.traveller.inventory.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

@Entity
@Table(name = "demand_metrics", indexes = {
    @Index(name = "idx_demand_room_date", columnList = "room_id, metric_date"),
    @Index(name = "idx_demand_hotel_date", columnList = "hotel_id, metric_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false)
    private Long roomId;

    @Column(name = "hotel_id", nullable = false)
    private Long hotelId;

    @Column(nullable = false)
    private LocalDate metricDate;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    @Builder.Default
    private Double occupancyRate = 0.0; // Percentage

    @Min(0)
    @Builder.Default
    private Integer bookingsCount = 0;

    @Min(0)
    @Builder.Default
    private Integer cancelledCount = 0;

    @Min(0)
    @Builder.Default
    private Integer viewCount = 0;

    @Min(0)
    @Builder.Default
    private Integer inquiryCount = 0;

    @Min(0)
    @Builder.Default
    private Double averageBookingValue = 0.0;

    @Min(0)
    @Builder.Default
    private Integer daysSinceLastBooking = 0; // Null = never booked

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DemandLevel demandLevel = DemandLevel.LOW; // LOW, MEDIUM, HIGH, VERY_HIGH

    @DecimalMin("0.5")
    @Builder.Default
    private Double priceMultiplier = 1.0; // Dynamic pricing adjustment (0.5 = 50% off, 2.0 = double)

    private String notes;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public enum DemandLevel {
        LOW,        // < 30% occupancy
        MEDIUM,     // 30-60% occupancy
        HIGH,       // 60-85% occupancy
        VERY_HIGH   // > 85% occupancy
    }

    /**
     * Calculate demand level based on occupancy rate
     */
    public void calculateDemandLevel() {
        if (occupancyRate < 30) {
            demandLevel = DemandLevel.LOW;
            priceMultiplier = 0.7; // 30% discount
        } else if (occupancyRate < 60) {
            demandLevel = DemandLevel.MEDIUM;
            priceMultiplier = 1.0; // Base price
        } else if (occupancyRate < 85) {
            demandLevel = DemandLevel.HIGH;
            priceMultiplier = 1.3; // 30% premium
        } else {
            demandLevel = DemandLevel.VERY_HIGH;
            priceMultiplier = 1.6; // 60% premium
        }
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Calculate booking velocity (bookings in last 24 hours trend)
     */
    public Double getBookingVelocity() {
        if (bookingsCount == 0) return 0.0;
        return (double) (bookingsCount - cancelledCount) / (daysSinceLastBooking > 0 ? daysSinceLastBooking : 1);
    }

    /**
     * Calculate cancellation rate
     */
    public Double getCancellationRate() {
        if (bookingsCount == 0) return 0.0;
        return (double) cancelledCount / bookingsCount * 100;
    }
}
