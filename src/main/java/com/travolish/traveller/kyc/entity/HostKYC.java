package com.travolish.traveller.kyc.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "host_kyc", indexes = {
    @Index(name = "idx_host_id", columnList = "host_id"),
    @Index(name = "idx_status", columnList = "kyc_status"),
    @Index(name = "idx_verification_level", columnList = "verification_level")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostKYC {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "host_id", nullable = false, unique = true)
    private Long hostId;
    
    // Personal Information
    @Column(name = "first_name")
    private String firstName;
    
    @Column(name = "last_name")
    private String lastName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    
    @Column(name = "phone_number")
    private String phoneNumber;
    
    @Column(name = "nationality")
    private String nationality;
    
    @Column(name = "national_id_number")
    private String nationalIdNumber;
    
    // Address Information
    @Column(name = "address_line1")
    private String addressLine1;
    
    @Column(name = "address_line2")
    private String addressLine2;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "state_province")
    private String stateProvince;
    
    @Column(name = "postal_code")
    private String postalCode;
    
    @Column(name = "country")
    private String country;
    
    // Business Information
    @Column(name = "business_name")
    private String businessName;
    
    @Column(name = "business_type")
    private String businessType; // Individual, Company, Partnership
    
    @Column(name = "business_registration_number")
    private String businessRegistrationNumber;
    
    @Column(name = "tax_id")
    private String taxId;
    
    @Column(name = "business_license_number")
    private String businessLicenseNumber;
    
    // Verification Status
    @Column(name = "kyc_status", nullable = false)
    private String kycStatus; // PENDING, UNDER_REVIEW, VERIFIED, REJECTED, EXPIRED
    
    @Column(name = "verification_level", nullable = false)
    private String verificationLevel; // BASIC, STANDARD, PREMIUM
    
    @Column(name = "verification_date")
    private LocalDateTime verificationDate;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    // Rejection/Feedback
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(name = "rejection_date")
    private LocalDateTime rejectionDate;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    // Risk Assessment
    @Column(name = "risk_score")
    private Integer riskScore; // 0-100, where 100 is highest risk
    
    @Column(name = "risk_level")
    private String riskLevel; // LOW, MEDIUM, HIGH
    
    // Document tracking
    @OneToMany(mappedBy = "hostKYC", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HostDocument> documents = new ArrayList<>();
    
    // Bank account tracking
    @OneToMany(mappedBy = "hostKYC", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<HostBankAccount> bankAccounts = new ArrayList<>();
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
