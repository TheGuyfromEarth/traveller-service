package com.travolish.traveller.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Step 1 of email sign-up: request a verification code be sent to an email.
 */
@Data
public class EmailSignupStartRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "A valid email is required")
    private String email;

    // Optional: "guest" or "host" — defaults to guest when absent
    private String role;
}
