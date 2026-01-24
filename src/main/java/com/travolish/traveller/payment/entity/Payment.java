package com.travolish.traveller.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_booking_id", columnList = "booking_id"),
    @Index(name = "idx_payment_status", columnList = "payment_status"),
    @Index(name = "idx_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_created_date", columnList = "created_at"),
    @Index(name = "idx_razorpay_order_id", columnList = "razorpay_order_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // References
    private Long userId;
    private Long bookingId;
    private Long paymentMethodId;
    
    // Payment Details
    private BigDecimal amount;
    private String currency;                        // USD, INR, etc.
    private String description;
    
    // Payment Status
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;            // PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED, REFUNDED
    
    // Razorpay Integration
    private String razorpayOrderId;                 // Razorpay order ID
    private String razorpayPaymentId;               // Razorpay payment ID
    private String razorpaySignature;               // Payment signature for verification
    
    // Transaction Details
    private String transactionId;                   // Internal transaction reference
    private String transactionReference;            // External gateway reference
    private String paymentMethod;                   // CARD, UPI, NETBANKING, WALLET, etc.
    private String cardLast4;                       // Last 4 digits of card
    private String bankName;                        // For bank transfers
    
    // Amount Details
    private BigDecimal baseAmount;                  // Original amount
    private BigDecimal taxAmount;                   // Tax/GST
    private BigDecimal platformFee;                 // Platform fee
    private BigDecimal totalAmount;                 // Total amount charged
    private BigDecimal netAmount;                   // Amount credited to host
    
    // Timestamps
    private LocalDateTime initiatedAt;
    private LocalDateTime processedAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
    
    // Failure Details
    private String failureReason;
    private String errorCode;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime lastRetryAt;
    
    // Receipt
    private Long receiptId;
    private String receiptUrl;
    private LocalDateTime receiptGeneratedAt;
    
    // Security
    private String ipAddress;
    private String userAgent;
    private Boolean isSecure;                       // SSL/3D Secure
    private String paymentSource;                   // WEB, MOBILE, API
    
    // Metadata
    private String notes;
    private String metadata;                        // JSON field for additional data
    
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
        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.PENDING;
        }
    }
    
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
