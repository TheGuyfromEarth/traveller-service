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
public class PaymentDTO {
    
    private Long id;
    private Long userId;
    private Long bookingId;
    private Long paymentMethodId;
    
    private BigDecimal amount;
    private String currency;
    private String description;
    
    private String paymentStatus;
    
    private String razorpayOrderId;
    private String razorpayPaymentId;
    
    private String transactionId;
    private String transactionReference;
    private String paymentMethod;
    private String cardLast4;
    
    private BigDecimal baseAmount;
    private BigDecimal taxAmount;
    private BigDecimal platformFee;
    private BigDecimal totalAmount;
    private BigDecimal netAmount;
    
    private LocalDateTime initiatedAt;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    
    private String failureReason;
    private String errorCode;
    
    private Long receiptId;
    private String receiptUrl;
    
    private String notes;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
