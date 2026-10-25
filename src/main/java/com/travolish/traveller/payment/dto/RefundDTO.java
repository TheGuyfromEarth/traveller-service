package com.travolish.traveller.payment.dto;

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
public class RefundDTO {
    
    private Long id;
    private Long paymentId;
    private Long userId;
    private Long bookingId;
    
    private BigDecimal refundAmount;
    private String currency;
    private String reason;
    
    private String refundStatus;
    
    private String razorpayRefundId;
    private String razorpayPaymentId;
    
    private String transactionReference;
    private String notes;
    
    private LocalDateTime requestedAt;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    
    private Integer retryCount;
    private String lastErrorMessage;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
