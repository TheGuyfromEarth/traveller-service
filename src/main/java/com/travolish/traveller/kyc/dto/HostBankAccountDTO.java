package com.travolish.traveller.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HostBankAccountDTO {
    private Long id;
    
    // Account Information
    private String bankName;
    private String accountHolderName;
    private String accountType;
    private String accountNumber;
    private String swiftCode;
    private String iban;
    private String routingNumber;
    private String country;
    private String currency;
    
    // Verification
    private String verificationStatus;
    private String verificationMethod;
    private Integer verificationAttempts;
    private LocalDateTime verifiedAt;
    
    // Status
    private Boolean isActive;
    private Boolean isPrimary;
    private LocalDateTime lastUsedDate;
    
    // Document verification
    private Boolean documentVerified;
}
