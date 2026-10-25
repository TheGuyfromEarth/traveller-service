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
public class GuestKYCDTO {
    private Long id;
    private Long guestId;

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

    // Verification Status
    private String kycStatus;
    private String verificationLevel;
    private LocalDateTime verificationDate;
    private LocalDate expiryDate;

    // Rejection / Feedback
    private String rejectionReason;
    private LocalDateTime rejectionDate;
    private String notes;
    private Long reviewerId;

    // Risk Assessment
    private Integer riskScore;
    private String riskLevel;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Related documents (populated on profile endpoint, null on list/status endpoints)
    private List<GuestDocumentDTO> documents;
}
