package com.travolish.traveller.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostEarningsDTO {
    private Long id;
    private Long hostId;
    private Long bookingId;
    private Long hotelId;
    private Long roomId;
    private Long guestId;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer numberOfNights;
    
    // Revenue breakdown
    private BigDecimal baseAmount;
    private BigDecimal taxesAndFees;
    private BigDecimal grossEarnings;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private BigDecimal discountsApplied;
    private BigDecimal netEarnings;
    
    // Status
    private String status; // PENDING, EARNED, PAID, REFUNDED
    private LocalDate earnedDate;
    private LocalDate paidDate;
    private Long payoutId;
    
    // Additional info
    private String currency;
    private String notes;
}
