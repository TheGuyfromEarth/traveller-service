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
public class PayoutStatusDTO {
    private Long payoutId;
    private String status; // PENDING, APPROVED, PROCESSING, COMPLETED, FAILED
    private BigDecimal amount;
    private BigDecimal netAmount;
    private LocalDateTime requestedDate;
    private LocalDateTime completedDate;
    private LocalDate expectedCompletionDate;
    private String transactionReference;
    private String failureReason;
    private Integer retryCount;
    private Integer progressPercentage;
}
