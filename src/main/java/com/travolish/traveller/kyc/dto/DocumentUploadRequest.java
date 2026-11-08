package com.travolish.traveller.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {
    private String documentType; // PASSPORT, ID, LICENSE, etc.
    private String documentName;
    private String fileUrl; // Pre-signed S3 URL after upload
    private String documentNumber;
    private String issuedDate;
    private String expiryDate;
    private String issuingCountry;
}
