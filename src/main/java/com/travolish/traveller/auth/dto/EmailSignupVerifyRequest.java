package com.travolish.traveller.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Step 2 of email sign-up: verify the emailed code and create the account.
 */
@Data
public class EmailSignupVerifyRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "A valid email is required")
    private String email;

    @NotBlank(message = "Verification code is required")
    private String code;

    // Optional profile fields collected alongside the code.
    // Exactly one name style should be provided:
    //   Nickname mode  → preferredName only (firstName/lastName omitted)
    //   Formal mode    → firstName + lastName (preferredName omitted)
    private String firstName;
    private String lastName;
    private String preferredName;

    @Pattern(regexp = "^\\+[1-9]\\d{7,14}$", message = "Phone must be E.164 format, e.g. +919876543210")
    private String phone;

    private Boolean whatsappOptIn = false;
}
