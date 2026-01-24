package com.travolish.traveller.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostKYCDTO {
    private Long id;
    private Long hostId;
    
    // Personal Information
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String phoneNumber;
    private String nationality;
    private String nationalIdNumber;
    
    // Address Information
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String stateProvince;
    private String postalCode;
    private String country;
    
    // Business Information
    private String businessName;
    private String businessType;
    private String businessRegistrationNumber;
    private String taxId;
    private String businessLicenseNumber;
    
    // Verification Status
    private String kycStatus;
    private String verificationLevel;
    private LocalDateTime verificationDate;
    private LocalDate expiryDate;
    
    // Risk Assessment
    private Integer riskScore;
    private String riskLevel;
    
    // Related data
    private List<HostDocumentDTO> documents;
    private List<HostBankAccountDTO> bankAccounts;
}
