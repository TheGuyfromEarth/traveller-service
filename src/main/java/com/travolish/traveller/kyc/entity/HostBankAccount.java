package com.travolish.traveller.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "host_bank_accounts", indexes = {
    @Index(name = "idx_bank_kyc_id", columnList = "host_kyc_id"),
    @Index(name = "idx_bank_status", columnList = "verification_status"),
    @Index(name = "idx_bank_is_primary", columnList = "is_primary")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostBankAccount {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_kyc_id", nullable = false)
    private HostKYC hostKYC;
    
    // Account Information
    @Column(name = "bank_name", nullable = false)
    private String bankName;
    
    @Column(name = "account_holder_name", nullable = false)
    private String accountHolderName;
    
    @Column(name = "account_type")
    private String accountType; // SAVINGS, CHECKING, BUSINESS
    
    @Column(name = "account_number", nullable = false)
    private String accountNumber;
    
    @Column(name = "swift_code")
    private String swiftCode;
    
    @Column(name = "iban")
    private String iban;
    
    @Column(name = "routing_number")
    private String routingNumber;
    
    @Column(name = "country", nullable = false)
    private String country;
    
    @Column(name = "currency", nullable = false)
    private String currency; // USD, EUR, GBP, etc.
    
    // Verification
    @Column(name = "verification_status", nullable = false)
    private String verificationStatus; // PENDING, VERIFIED, FAILED, LOCKED
    
    @Column(name = "verification_method")
    private String verificationMethod; // MICRO_DEPOSIT, DOCUMENT, INSTANT
    
    @Column(name = "verification_attempts")
    private Integer verificationAttempts = 0;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    // Micro-deposit verification
    @Column(name = "micro_deposit_amount1")
    private Double microDepositAmount1;
    
    @Column(name = "micro_deposit_amount2")
    private Double microDepositAmount2;
    
    @Column(name = "micro_deposit_valid_until")
    private LocalDateTime microDepositValidUntil;
    
    @Column(name = "micro_deposit_verified")
    private Boolean microDepositVerified = false;
    
    // Status
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;
    
    @Column(name = "last_used_date")
    private LocalDateTime lastUsedDate;
    
    // Document verification
    @Column(name = "bank_statement_url")
    private String bankStatementUrl;
    
    @Column(name = "document_verified")
    private Boolean documentVerified = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
