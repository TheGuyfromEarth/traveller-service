package com.travolish.traveller.payout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutRequest {
    
    @NotNull(message = "Bank account ID is required")
    private Long bankAccountId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private LocalDate payoutPeriodStart;
    private LocalDate payoutPeriodEnd;
    
    private String description;
    private String notes;
    
    // Optional: Specific bookings to include
    private String bookingIds;
}
