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

    // Optional profile fields collected alongside the code.
    // Exactly one name style should be provided:
    //   Nickname mode  → preferredName only (firstName/lastName omitted)
    //   Formal mode    → firstName + lastName (preferredName omitted)
    private String firstName;
    private String lastName;
    private String preferredName;

    // TODO (WhatsApp — Phase 2): Collect phone number at sign-up for WhatsApp notifications.
    //   • Add `@Pattern(regexp = "^\\+[1-9]\\d{7,14}$")` validation for E.164 format.
    //     Meta Cloud API expects the recipient number without any prefix (plain E.164, e.g. "919876543210").
    //   • This field is optional at sign-up; users without a phone skip the WhatsApp flow entirely.
    //   File: EmailSignupService.verifyAndCreate() — persist this value to User.phone.
    private String phone;

    // TODO (WhatsApp — Phase 2): Explicit WhatsApp opt-in consent.
    //   • Meta policy + GDPR: users must actively agree before receiving WhatsApp messages.
    //     Sending to a number without opt-in can get the WhatsApp Business Account suspended by Meta.
    //   • Only send WhatsApp notifications when this is true AND phone is present.
    //   • Store consent timestamp in a separate `whatsapp_opt_ins` audit table (Phase 5).
    //   File: WhatsAppConsentService (to be created in notifications/service/).
    private Boolean whatsappOptIn = false;
}
