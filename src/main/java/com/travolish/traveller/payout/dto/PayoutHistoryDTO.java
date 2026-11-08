package com.travolish.traveller.payout.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutHistoryDTO {
    private Long id;
    private BigDecimal amount;
    private BigDecimal netAmount;
    private String status;
    private LocalDateTime requestedDate;
    private LocalDateTime completedDate;
    private LocalDate payoutPeriodStart;
    private LocalDate payoutPeriodEnd;
    private Integer bookingCount;
    private String description;
}
