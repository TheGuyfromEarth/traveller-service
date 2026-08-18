package com.travolish.traveller.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "refunds", indexes = {
    @Index(name = "idx_refund_payment_id", columnList = "payment_id"),
    @Index(name = "idx_refund_user_id", columnList = "user_id"),
    @Index(name = "idx_refund_status", columnList = "refund_status"),
    @Index(name = "idx_razorpay_refund_id", columnList = "razorpay_refund_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // References
    private Long paymentId;
    private Long userId;
    private Long bookingId;
    
    // Refund Details
    private BigDecimal refundAmount;
    private String currency;
    private String reason;                          // Booking cancellation, Guest request, Dispute resolution, etc.
    
    // Refund Status
    @Enumerated(EnumType.STRING)
    private RefundStatus refundStatus;              // PENDING, PROCESSING, COMPLETED, FAILED
    
    // Razorpay Integration
    private String razorpayRefundId;                // Razorpay refund ID
    private String razorpayPaymentId;               // Associated Razorpay payment ID
    
    // Transaction Details
    private String transactionReference;
    private String notes;
    
    // Timestamps
    private LocalDateTime requestedAt;
    private LocalDateTime initiatedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    
    // Retry Logic
    private Integer retryCount;
    private LocalDateTime lastRetryAt;
    private String lastErrorMessage;
    
    // Audit
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private Boolean isDeleted;
    
    @PrePersist
    public void onPrePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isDeleted = false;
        this.retryCount = 0;
        this.requestedAt = LocalDateTime.now();
        if (this.refundStatus == null) {
            this.refundStatus = RefundStatus.PENDING;
        }
    }
    
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
