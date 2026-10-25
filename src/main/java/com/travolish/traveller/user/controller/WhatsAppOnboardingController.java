package com.travolish.traveller.user.controller;

import com.travolish.traveller.notifications.service.PhoneVerificationService;
import com.travolish.traveller.notifications.whatsapp.WhatsAppConsentService;
import com.travolish.traveller.notifications.whatsapp.WhatsAppOptInSource;
import com.travolish.traveller.user.dto.PhoneOtpVerifyRequest;
import com.travolish.traveller.user.dto.WhatsAppOptInRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class WhatsAppOnboardingController {

    private final WhatsAppConsentService consentService;
    private final PhoneVerificationService phoneVerificationService;

    @PostMapping("/whatsapp-opt-in")
    public ResponseEntity<Void> whatsAppOptIn(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody WhatsAppOptInRequest body,
            HttpServletRequest request) {
        Long userId = Long.parseLong(jwt.getSubject());
        if (!Boolean.TRUE.equals(body.getWhatsappOptIn())) {
            consentService.revoke(userId);
            return ResponseEntity.noContent().build();
        }
        if (body.getPhone() == null || body.getPhone().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        consentService.recordOptIn(
                userId,
                body.getPhone(),
                WhatsAppOptInSource.ONBOARDING_API,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/phone/send-otp")
    public ResponseEntity<Void> sendPhoneOtp(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PhoneOtpVerifyRequest body) {
        Long userId = Long.parseLong(jwt.getSubject());
        if (body.getPhone() == null || body.getPhone().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        phoneVerificationService.sendOtp(userId, body.getPhone());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/phone/verify-otp")
    public ResponseEntity<Void> verifyPhoneOtp(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PhoneOtpVerifyRequest body) {
        Long userId = Long.parseLong(jwt.getSubject());
        if (body.getPhone() == null || body.getCode() == null) {
            return ResponseEntity.badRequest().build();
        }
        phoneVerificationService.verifyOtp(userId, body.getPhone(), body.getCode());
        return ResponseEntity.noContent().build();
    }
}
