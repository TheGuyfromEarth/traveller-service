package com.travolish.traveller.inventory.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandMetricsDTO {

    private Long id;

    private Long roomId;

    private Long hotelId;

    private LocalDate metricDate;

    private Double occupancyRate;

    private Integer bookingsCount;

    private Integer cancelledCount;

    private Integer viewCount;

    private Integer inquiryCount;

    private Double averageBookingValue;

    private Integer daysSinceLastBooking;

    private String demandLevel; // LOW, MEDIUM, HIGH, VERY_HIGH

    private Double priceMultiplier;

    private String notes;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;

    /**
     * Calculate booking velocity
     */
    public Double getBookingVelocity() {
        if (bookingsCount == 0) return 0.0;
        return (double) (bookingsCount - (cancelledCount != null ? cancelledCount : 0)) 
               / (daysSinceLastBooking != null && daysSinceLastBooking > 0 ? daysSinceLastBooking : 1);
    }

    /**
     * Calculate cancellation rate
     */
    public Double getCancellationRate() {
        if (bookingsCount == 0) return 0.0;
        return (double) (cancelledCount != null ? cancelledCount : 0) / bookingsCount * 100;
    }

    /**
     * Calculate conversion rate (bookings vs views)
     */
    public Double getConversionRate() {
        if (viewCount == null || viewCount == 0) return 0.0;
        return (double) bookingsCount / viewCount * 100;
    }

    /**
     * Get metrics summary
     */
    public String getSummary() {
        return String.format(
            "Room %d (%s): %.1f%% occupancy, %.1fx multiplier, %d bookings, %.1f%% conversion",
            roomId, demandLevel, occupancyRate, priceMultiplier, bookingsCount, getConversionRate()
        );
    }
}
