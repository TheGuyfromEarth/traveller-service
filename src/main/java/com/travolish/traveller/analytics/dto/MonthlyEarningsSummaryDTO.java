package com.travolish.traveller.analytics.dto;

import java.math.BigDecimal;

public class MonthlyEarningsSummaryDTO {
    private Integer month;
    private Integer year;
    private BigDecimal totalEarnings;
    private BigDecimal grossRevenue;
    private BigDecimal commissionPaid;
    private BigDecimal netEarnings;
    private Integer completedBookings;
    private BigDecimal averageBookingValue;
    private Integer totalGuests;

    // Constructors
    public MonthlyEarningsSummaryDTO() {}

    public MonthlyEarningsSummaryDTO(Integer month, Integer year, BigDecimal totalEarnings, BigDecimal grossRevenue,
                                    BigDecimal commissionPaid, BigDecimal netEarnings, Integer completedBookings,
                                    BigDecimal averageBookingValue, Integer totalGuests) {
        this.month = month;
        this.year = year;
        this.totalEarnings = totalEarnings;
        this.grossRevenue = grossRevenue;
        this.commissionPaid = commissionPaid;
        this.netEarnings = netEarnings;
        this.completedBookings = completedBookings;
        this.averageBookingValue = averageBookingValue;
        this.totalGuests = totalGuests;
    }

    // Getters and Setters
    public Integer getMonth() { return month; }
    public void setMonth(Integer month) { this.month = month; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }

    public BigDecimal getGrossRevenue() { return grossRevenue; }
    public void setGrossRevenue(BigDecimal grossRevenue) { this.grossRevenue = grossRevenue; }

    public BigDecimal getCommissionPaid() { return commissionPaid; }
    public void setCommissionPaid(BigDecimal commissionPaid) { this.commissionPaid = commissionPaid; }

    public BigDecimal getNetEarnings() { return netEarnings; }
    public void setNetEarnings(BigDecimal netEarnings) { this.netEarnings = netEarnings; }

    public Integer getCompletedBookings() { return completedBookings; }
    public void setCompletedBookings(Integer completedBookings) { this.completedBookings = completedBookings; }

    public BigDecimal getAverageBookingValue() { return averageBookingValue; }
    public void setAverageBookingValue(BigDecimal averageBookingValue) { this.averageBookingValue = averageBookingValue; }

    public Integer getTotalGuests() { return totalGuests; }
    public void setTotalGuests(Integer totalGuests) { this.totalGuests = totalGuests; }
}
