package com.travolish.traveller.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostDocumentDTO {
    private Long id;
    private String documentType;
    private String documentName;
    private String fileUrl;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String documentNumber;
    private LocalDate issuedDate;
    private LocalDate expiryDate;
    private String issuingCountry;
    
    // Verification
    private String verificationStatus;
    private String verificationNotes;
    private LocalDateTime verifiedAt;
    
    // AI Verification
    private Boolean aiVerified;
    private Double aiConfidenceScore;
}
