package com.travolish.traveller.analytics.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "host_analytics", indexes = {
    @Index(name = "idx_analytics_host_id", columnList = "host_id"),
    @Index(name = "idx_date", columnList = "analytics_date"),
    @Index(name = "idx_period", columnList = "period_start_date, period_end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "host_id", nullable = false)
    private Long hostId;
    
    @Column(name = "analytics_date", nullable = false)
    private LocalDate analyticsDate;
    
    @Column(name = "period_start_date")
    private LocalDate periodStartDate;
    
    @Column(name = "period_end_date")
    private LocalDate periodEndDate;
    
    // Booking metrics
    @Column(name = "total_bookings")
    private Integer totalBookings = 0;
    
    @Column(name = "completed_bookings")
    private Integer completedBookings = 0;
    
    @Column(name = "cancelled_bookings")
    private Integer cancelledBookings = 0;
    
    @Column(name = "pending_bookings")
    private Integer pendingBookings = 0;
    
    // Occupancy metrics
    @Column(name = "occupancy_rate", precision = 5, scale = 2)
    private BigDecimal occupancyRate = BigDecimal.ZERO;
    
    @Column(name = "occupied_nights")
    private Integer occupiedNights = 0;
    
    @Column(name = "total_available_nights")
    private Integer totalAvailableNights = 0;
    
    // Revenue metrics
    @Column(name = "total_revenue", precision = 15, scale = 2)
    private BigDecimal totalRevenue = BigDecimal.ZERO;
    
    @Column(name = "gross_revenue", precision = 15, scale = 2)
    private BigDecimal grossRevenue = BigDecimal.ZERO;
    
    @Column(name = "net_revenue", precision = 15, scale = 2)
    private BigDecimal netRevenue = BigDecimal.ZERO;
    
    @Column(name = "commission_paid", precision = 15, scale = 2)
    private BigDecimal commissionPaid = BigDecimal.ZERO;
    
    // Guest metrics
    @Column(name = "total_guests")
    private Integer totalGuests = 0;
    
    @Column(name = "returning_guests")
    private Integer returningGuests = 0;
    
    @Column(name = "unique_guests")
    private Integer uniqueGuests = 0;
    
    // Review metrics
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;
    
    @Column(name = "total_reviews")
    private Integer totalReviews = 0;
    
    @Column(name = "five_star_reviews")
    private Integer fiveStarReviews = 0;
    
    // Performance metrics
    @Column(name = "response_rate", precision = 5, scale = 2)
    private BigDecimal responseRate = BigDecimal.ZERO;
    
    @Column(name = "average_response_time_hours")
    private Integer averageResponseTimeHours = 0;
    
    @Column(name = "cancellation_rate", precision = 5, scale = 2)
    private BigDecimal cancellationRate = BigDecimal.ZERO;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Explicit getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getHostId() { return hostId; }
    public void setHostId(Long hostId) { this.hostId = hostId; }

    public LocalDate getAnalyticsDate() { return analyticsDate; }
    public void setAnalyticsDate(LocalDate analyticsDate) { this.analyticsDate = analyticsDate; }

    public LocalDate getPeriodStartDate() { return periodStartDate; }
    public void setPeriodStartDate(LocalDate periodStartDate) { this.periodStartDate = periodStartDate; }

    public LocalDate getPeriodEndDate() { return periodEndDate; }
    public void setPeriodEndDate(LocalDate periodEndDate) { this.periodEndDate = periodEndDate; }

    public Integer getTotalBookings() { return totalBookings; }
    public void setTotalBookings(Integer totalBookings) { this.totalBookings = totalBookings; }

    public Integer getCompletedBookings() { return completedBookings; }
    public void setCompletedBookings(Integer completedBookings) { this.completedBookings = completedBookings; }

    public Integer getCancelledBookings() { return cancelledBookings; }
    public void setCancelledBookings(Integer cancelledBookings) { this.cancelledBookings = cancelledBookings; }

    public Integer getPendingBookings() { return pendingBookings; }
    public void setPendingBookings(Integer pendingBookings) { this.pendingBookings = pendingBookings; }

    public BigDecimal getOccupancyRate() { return occupancyRate; }
    public void setOccupancyRate(BigDecimal occupancyRate) { this.occupancyRate = occupancyRate; }

    public Integer getOccupiedNights() { return occupiedNights; }
    public void setOccupiedNights(Integer occupiedNights) { this.occupiedNights = occupiedNights; }

    public Integer getTotalAvailableNights() { return totalAvailableNights; }
    public void setTotalAvailableNights(Integer totalAvailableNights) { this.totalAvailableNights = totalAvailableNights; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(BigDecimal grossRevenue) { this.grossRevenue = grossRevenue; }

    public BigDecimal getNetRevenue() { return netRevenue; }
    public void setNetRevenue(BigDecimal netRevenue) { this.netRevenue = netRevenue; }

    public BigDecimal getCommissionPaid() { return commissionPaid; }
    public void setCommissionPaid(BigDecimal commissionPaid) { this.commissionPaid = commissionPaid; }

    public Integer getTotalGuests() { return totalGuests; }
    public void setTotalGuests(Integer totalGuests) { this.totalGuests = totalGuests; }

    public Integer getReturningGuests() { return returningGuests; }
    public void setReturningGuests(Integer returningGuests) { this.returningGuests = returningGuests; }

    public Integer getUniqueGuests() { return uniqueGuests; }
    public void setUniqueGuests(Integer uniqueGuests) { this.uniqueGuests = uniqueGuests; }

    public BigDecimal getAverageRating() { return averageRating; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }

    public Integer getTotalReviews() { return totalReviews; }
    public void setTotalReviews(Integer totalReviews) { this.totalReviews = totalReviews; }

    public Integer getFiveStarReviews() { return fiveStarReviews; }
    public void setFiveStarReviews(Integer fiveStarReviews) { this.fiveStarReviews = fiveStarReviews; }

    public BigDecimal getResponseRate() { return responseRate; }
    public void setResponseRate(BigDecimal responseRate) { this.responseRate = responseRate; }

    public Integer getAverageResponseTimeHours() { return averageResponseTimeHours; }
    public void setAverageResponseTimeHours(Integer averageResponseTimeHours) { this.averageResponseTimeHours = averageResponseTimeHours; }

    public BigDecimal getCancellationRate() { return cancellationRate; }
    public void setCancellationRate(BigDecimal cancellationRate) { this.cancellationRate = cancellationRate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
