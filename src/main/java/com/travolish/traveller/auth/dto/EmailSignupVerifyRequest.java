package com.travolish.traveller.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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

    // Optional profile fields collected alongside the code
    private String firstName;
    private String lastName;
}
