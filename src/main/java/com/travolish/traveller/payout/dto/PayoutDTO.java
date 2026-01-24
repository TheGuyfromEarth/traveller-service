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
public class PayoutDTO {
    private Long id;
    private Long hostId;
    private Long bankAccountId;
    private BigDecimal amount;
    private BigDecimal grossAmount;
    private BigDecimal commissionAmount;
    private BigDecimal taxesAmount;
    private BigDecimal payoutFees;
    private BigDecimal netAmount;
    private String payoutStatus;
    private LocalDateTime requestedDate;
    private LocalDateTime approvedDate;
    private LocalDateTime processedDate;
    private LocalDateTime completedDate;
    private String failureReason;
    private Integer retryCount;
    private String transactionReference;
    private String paymentMethod;
    private LocalDate expectedCompletionDate;
    private LocalDate actualCompletionDate;
    private LocalDate payoutPeriodStart;
    private LocalDate payoutPeriodEnd;
    private Integer bookingCount;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
