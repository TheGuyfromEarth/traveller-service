package com.travolish.traveller.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "receipts", indexes = {
    @Index(name = "idx_payment_id", columnList = "payment_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_booking_id", columnList = "booking_id"),
    @Index(name = "idx_created_date", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Receipt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // References
    private Long paymentId;
    private Long userId;
    private Long bookingId;
    private Long hostId;
    
    // Receipt Details
    private String receiptNumber;                   // Unique receipt identifier
    @Enumerated(EnumType.STRING)
    private ReceiptStatus receiptStatus;            // DRAFT, GENERATED, SENT, DOWNLOADED
    
    // Amount Details
    private BigDecimal baseAmount;
    private BigDecimal taxAmount;
    private BigDecimal platformFee;
    private BigDecimal totalAmount;
    
    // Document
    private String pdfUrl;
    private String htmlUrl;
    private String fileKey;                         // S3/Cloud storage key
    
    // Recipient Details
    private String recipientEmail;
    private String recipientName;
    
    // Timestamps
    private LocalDateTime generatedAt;
    private LocalDateTime sentAt;
    private LocalDateTime downloadedAt;
    
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
        if (this.receiptStatus == null) {
            this.receiptStatus = ReceiptStatus.DRAFT;
        }
    }
    
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
