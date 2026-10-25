package com.travolish.traveller.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostAnalyticsDTO {
    private Long id;
    private Long hostId;
    private LocalDate analyticsDate;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    
    // Booking metrics
    private Integer totalBookings;
    private Integer completedBookings;
    private Integer cancelledBookings;
    private Integer pendingBookings;
    
    // Occupancy metrics
    private BigDecimal occupancyRate;
    private Integer occupiedNights;
    private Integer totalAvailableNights;
    
    // Revenue metrics
    private BigDecimal totalRevenue;
    private BigDecimal grossRevenue;
    private BigDecimal netRevenue;
    private BigDecimal commissionPaid;
    
    // Guest metrics
    private Integer totalGuests;
    private Integer returningGuests;
    private Integer uniqueGuests;
    
    // Review metrics
    private BigDecimal averageRating;
    private Integer totalReviews;
    private Integer fiveStarReviews;
    
    // Performance metrics
    private BigDecimal responseRate;
    private Integer averageResponseTimeHours;
    private BigDecimal cancellationRate;
}
