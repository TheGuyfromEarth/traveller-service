package com.travolish.traveller.payout.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payouts", indexes = {
    @Index(name = "idx_host_id", columnList = "host_id"),
    @Index(name = "idx_payout_status", columnList = "payout_status"),
    @Index(name = "idx_requested_date", columnList = "requested_date"),
    @Index(name = "idx_processed_date", columnList = "processed_date"),
    @Index(name = "idx_bank_account_id", columnList = "bank_account_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payout {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long hostId;
    
    // Bank account reference (from KYC module)
    @Column(name = "bank_account_id")
    private Long bankAccountId;
    
    // Payout details
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "gross_amount", precision = 12, scale = 2)
    private BigDecimal grossAmount; // Before deductions
    
    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount; // Platform commission
    
    @Column(name = "taxes_amount", precision = 12, scale = 2)
    private BigDecimal taxesAmount; // Tax deductions
    
    @Column(name = "payout_fees", precision = 12, scale = 2)
    private BigDecimal payoutFees; // Bank/processing fees
    
    @Column(name = "net_amount", precision = 12, scale = 2)
    private BigDecimal netAmount; // Final amount paid
    
    // Status tracking
    @Column(nullable = false)
    private String payoutStatus; // PENDING, APPROVED, PROCESSING, COMPLETED, FAILED, CANCELLED
    
    @Column(name = "requested_date", nullable = false)
    private LocalDateTime requestedDate;
    
    @Column(name = "approved_date")
    private LocalDateTime approvedDate;
    
    @Column(name = "processed_date")
    private LocalDateTime processedDate;
    
    @Column(name = "completed_date")
    private LocalDateTime completedDate;
    
    // Failure handling
    @Column(name = "failure_reason")
    private String failureReason;
    
    @Column(name = "retry_count", columnDefinition = "INT DEFAULT 0")
    private Integer retryCount;
    
    @Column(name = "last_retry_date")
    private LocalDateTime lastRetryDate;
    
    // Payment details
    @Column(name = "transaction_reference")
    private String transactionReference; // Bank transaction ID
    
    @Column(name = "payment_method")
    private String paymentMethod; // BANK_TRANSFER, ACH, SWIFT, etc.
    
    @Column(name = "expected_completion_date")
    private LocalDate expectedCompletionDate;
    
    @Column(name = "actual_completion_date")
    private LocalDate actualCompletionDate;
    
    // Period information
    @Column(name = "payout_period_start")
    private LocalDate payoutPeriodStart;
    
    @Column(name = "payout_period_end")
    private LocalDate payoutPeriodEnd;
    
    // Booking references
    @Column(name = "booking_ids")
    private String bookingIds; // Comma-separated IDs of bookings included
    
    @Column(name = "booking_count", columnDefinition = "INT DEFAULT 0")
    private Integer bookingCount;
    
    // Metadata
    @Column(name = "description")
    private String description;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "metadata")
    private String metadata; // JSON metadata
    
    // Audit fields
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "is_deleted", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDeleted;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isDeleted = false;
        retryCount = 0;
        if (payoutStatus == null) {
            payoutStatus = "PENDING";
        }
        requestedDate = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
