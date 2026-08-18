package com.travolish.traveller.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuestDocumentUploadRequest {
    private String documentType;   // required: GOVERNMENT_ID | PROOF_OF_ADDRESS
    private String documentName;
    private String fileUrl;        // required
    private String documentNumber;
    private String issuedDate;     // ISO date string, parsed in service
    private String expiryDate;     // ISO date string, parsed in service
    private String issuingCountry;
}
