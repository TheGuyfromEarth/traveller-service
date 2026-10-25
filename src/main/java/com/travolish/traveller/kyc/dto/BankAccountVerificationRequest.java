package com.travolish.traveller.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountVerificationRequest {
    private String bankName;
    private String accountHolderName;
    private String accountType;
    private String accountNumber;
    private String swiftCode;
    private String iban;
    private String routingNumber;
    private String country;
    private String currency;
    private String verificationMethod; // MICRO_DEPOSIT, DOCUMENT, INSTANT
    private String bankStatementUrl; // For document verification
    private Boolean isPrimary;
}
