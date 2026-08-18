package com.travolish.traveller.payout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutBalanceDTO {
    private Long hostId;
    private BigDecimal availableBalance; // Ready for payout
    private BigDecimal pendingBalance;   // In pending payouts
    private BigDecimal totalEarnings;    // All-time earnings
    private BigDecimal totalPayouts;     // Total paid out
    private BigDecimal totalCommissions; // Total platform commission
    private BigDecimal totalTaxes;       // Total taxes paid
    private BigDecimal netEarnings;      // Earnings after deductions
    private LocalDateTime lastPayoutDate;
    private Integer nextPayoutDaysRemaining;
}
