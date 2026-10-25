package com.travolish.traveller.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationStatusDTO {
    private Long hostId;
    private String overallStatus; // PENDING, UNDER_REVIEW, VERIFIED, REJECTED, INCOMPLETE
    private String kycStatus;
    private String verificationLevel;
    private Integer documentsRequired;
    private Integer documentsSubmitted;
    private Integer documentsVerified;
    private Integer bankAccountsRequired;
    private Integer bankAccountsSubmitted;
    private Integer bankAccountsVerified;
    private Integer overallProgress;
    private Integer riskScore;
    private String riskLevel;
    private String nextSteps;
    private String rejectionReason;
}
