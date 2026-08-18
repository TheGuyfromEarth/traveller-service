package com.travolish.traveller.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardOverviewDTO {
    // Summary stats
    private BigDecimal totalEarnings;
    private BigDecimal thisMonthEarnings;
    private Integer totalBookings;
    private Integer thisMonthBookings;
    private BigDecimal averageRating;
    private BigDecimal occupancyRate;
    
    // Trending data
    private List<DailyMetricDTO> bookingsTrend;
    private List<DailyMetricDTO> revenueTrend;
    private List<DailyMetricDTO> occupancyTrend;
    
    // Recent activities
    private Integer pendingBookings;
    private Integer cancelledThisMonth;
    private Integer reviewsThisMonth;
    private Integer newGuestsThisMonth;
    
    // Performance indicators
    private BigDecimal responseRate;
    private BigDecimal cancellationRate;
    private Integer returningGuestPercentage;
    
    // Top performers
    private List<TopPerformerDTO> topRooms;
    private List<TopPerformerDTO> topReviews;

    // Constructors
    public DashboardOverviewDTO() {}

    public DashboardOverviewDTO(BigDecimal totalEarnings, BigDecimal thisMonthEarnings, Integer totalBookings,
                               Integer thisMonthBookings, BigDecimal averageRating, BigDecimal occupancyRate,
                               List<DailyMetricDTO> bookingsTrend, List<DailyMetricDTO> revenueTrend,
                               List<DailyMetricDTO> occupancyTrend, Integer pendingBookings, Integer cancelledThisMonth,
                               Integer reviewsThisMonth, Integer newGuestsThisMonth, BigDecimal responseRate,
                               BigDecimal cancellationRate, Integer returningGuestPercentage,
                               List<TopPerformerDTO> topRooms, List<TopPerformerDTO> topReviews) {
        this.totalEarnings = totalEarnings;
        this.thisMonthEarnings = thisMonthEarnings;
        this.totalBookings = totalBookings;
        this.thisMonthBookings = thisMonthBookings;
        this.averageRating = averageRating;
        this.occupancyRate = occupancyRate;
        this.bookingsTrend = bookingsTrend;
        this.revenueTrend = revenueTrend;
        this.occupancyTrend = occupancyTrend;
        this.pendingBookings = pendingBookings;
        this.cancelledThisMonth = cancelledThisMonth;
        this.reviewsThisMonth = reviewsThisMonth;
        this.newGuestsThisMonth = newGuestsThisMonth;
        this.responseRate = responseRate;
        this.cancellationRate = cancellationRate;
        this.returningGuestPercentage = returningGuestPercentage;
        this.topRooms = topRooms;
        this.topReviews = topReviews;
    }

    // Getters and Setters
    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }

    public BigDecimal getThisMonthEarnings() { return thisMonthEarnings; }
    public void setThisMonthEarnings(BigDecimal thisMonthEarnings) { this.thisMonthEarnings = thisMonthEarnings; }

    public Integer getTotalBookings() { return totalBookings; }
    public void setTotalBookings(Integer totalBookings) { this.totalBookings = totalBookings; }

    public Integer getThisMonthBookings() { return thisMonthBookings; }
    public void setThisMonthBookings(Integer thisMonthBookings) { this.thisMonthBookings = thisMonthBookings; }

    public BigDecimal getAverageRating() { return averageRating; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }

    public BigDecimal getOccupancyRate() { return occupancyRate; }
    public void setOccupancyRate(BigDecimal occupancyRate) { this.occupancyRate = occupancyRate; }

    public List<DailyMetricDTO> getBookingsTrend() { return bookingsTrend; }
    public void setBookingsTrend(List<DailyMetricDTO> bookingsTrend) { this.bookingsTrend = bookingsTrend; }

    public List<DailyMetricDTO> getRevenueTrend() { return revenueTrend; }
    public void setRevenueTrend(List<DailyMetricDTO> revenueTrend) { this.revenueTrend = revenueTrend; }

    public List<DailyMetricDTO> getOccupancyTrend() { return occupancyTrend; }
    public void setOccupancyTrend(List<DailyMetricDTO> occupancyTrend) { this.occupancyTrend = occupancyTrend; }

    public Integer getPendingBookings() { return pendingBookings; }
    public void setPendingBookings(Integer pendingBookings) { this.pendingBookings = pendingBookings; }

    public Integer getCancelledThisMonth() { return cancelledThisMonth; }
    public void setCancelledThisMonth(Integer cancelledThisMonth) { this.cancelledThisMonth = cancelledThisMonth; }

    public Integer getReviewsThisMonth() { return reviewsThisMonth; }
    public void setReviewsThisMonth(Integer reviewsThisMonth) { this.reviewsThisMonth = reviewsThisMonth; }

    public Integer getNewGuestsThisMonth() { return newGuestsThisMonth; }
    public void setNewGuestsThisMonth(Integer newGuestsThisMonth) { this.newGuestsThisMonth = newGuestsThisMonth; }

    public BigDecimal getResponseRate() { return responseRate; }
    public void setResponseRate(BigDecimal responseRate) { this.responseRate = responseRate; }

    public BigDecimal getCancellationRate() { return cancellationRate; }
    public void setCancellationRate(BigDecimal cancellationRate) { this.cancellationRate = cancellationRate; }

    public Integer getReturningGuestPercentage() { return returningGuestPercentage; }
    public void setReturningGuestPercentage(Integer returningGuestPercentage) { this.returningGuestPercentage = returningGuestPercentage; }

    public List<TopPerformerDTO> getTopRooms() { return topRooms; }
    public void setTopRooms(List<TopPerformerDTO> topRooms) { this.topRooms = topRooms; }

    public List<TopPerformerDTO> getTopReviews() { return topReviews; }
    public void setTopReviews(List<TopPerformerDTO> topReviews) { this.topReviews = topReviews; }
}
