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
public class TransactionDTO {
    
    private Long id;
    private String transactionId;
    private Long paymentId;
    private Long userId;
    
    private BigDecimal amount;
    private String currency;
    
    private String status;                          // PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED
    private String paymentMethod;
    
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String razorpaySignature;
    
    private String cardLast4;
    private String bankName;
    
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    
    private String failureReason;
    private String errorCode;
    
    private String ipAddress;
    private String userAgent;
    private Boolean isSecure;
    
    private LocalDateTime createdAt;
}
