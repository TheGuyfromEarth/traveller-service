package com.travolish.traveller.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_pay_method_user_id", columnList = "user_id"),
    @Index(name = "idx_razorpay_token_id", columnList = "razorpay_token_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // References
    private Long userId;
    
    // Payment Method Details
    @Enumerated(EnumType.STRING)
    private PaymentMethodType methodType;          // CARD, UPI, NETBANKING, WALLET
    
    private String methodName;                      // Human-readable name (e.g., "My Visa Card")
    private Boolean isDefault;
    private Boolean isActive;
    
    // Card Details (encrypted)
    private String cardLast4;
    private String cardNetwork;                     // VISA, MASTERCARD, AMEX, RUPay
    private String cardHolderName;
    private Integer cardExpiryMonth;
    private Integer cardExpiryYear;
    private String cardIssuerBank;
    private String cardType;                        // CREDIT, DEBIT
    
    // UPI Details
    private String upiHandle;
    private String upiVpa;
    
    // Razorpay Integration
    private String razorpayTokenId;                 // Saved payment token from Razorpay
    private String razorpayCustomerId;              // Razorpay customer ID
    
    // Verification
    private Boolean isVerified;
    private LocalDateTime verifiedAt;
    private String verificationMethod;
    
    // Usage Statistics
    private Integer usageCount;
    private LocalDateTime lastUsedAt;
    
    // Security
    private String ipAddress;
    private String userAgent;
    
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
        this.isDefault = false;
        this.isActive = true;
        this.isVerified = false;
        this.usageCount = 0;
    }
    
    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
