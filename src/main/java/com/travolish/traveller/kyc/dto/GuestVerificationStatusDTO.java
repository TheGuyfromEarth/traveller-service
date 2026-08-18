package com.travolish.traveller.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestVerificationStatusDTO {
    private Long guestId;
    private String overallStatus;    // NOT_STARTED, UNDER_REVIEW, VERIFIED, REJECTED, PENDING_FINAL_APPROVAL
    private String kycStatus;
    private String verificationLevel;
    private Integer documentsRequired;   // always 2: GOVERNMENT_ID + PROOF_OF_ADDRESS
    private Integer documentsSubmitted;
    private Integer documentsVerified;
    private Integer overallProgress;     // 0–100
    private Integer riskScore;
    private String riskLevel;
    private String rejectionReason;
    private String notes;
}
