package com.travolish.traveller.kyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitKYCRequest {
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
}
