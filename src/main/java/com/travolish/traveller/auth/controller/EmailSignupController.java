package com.travolish.traveller.auth.controller;

import com.travolish.traveller.auth.dto.EmailSignupStartRequest;
import com.travolish.traveller.auth.dto.EmailSignupVerifyRequest;
import com.travolish.traveller.auth.dto.OAuth2AuthResponse;
import com.travolish.traveller.auth.service.EmailSignupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Passwordless email sign-up endpoints. Lives under /api/auth/** which is public.
 *
 *  POST /api/auth/signup/start   { email, role? }            -> emails a 6-digit code
 *  POST /api/auth/signup/verify  { email, code, names? }     -> creates user, returns tokens
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/signup")
@RequiredArgsConstructor
public class EmailSignupController {

    private final EmailSignupService emailSignupService;

    @PostMapping("/start")
    public ResponseEntity<?> start(@Valid @RequestBody EmailSignupStartRequest request) {
        try {
            emailSignupService.startSignup(request);
            return ResponseEntity.ok(Map.of("message", "Verification code sent"));
        } catch (IllegalStateException e) {
            // Email already registered
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to start email signup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not send verification code. Please try again."));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@Valid @RequestBody EmailSignupVerifyRequest request) {
        try {
            OAuth2AuthResponse response = emailSignupService.verifyAndCreate(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            // Wrong / expired / missing code
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to verify email signup", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Could not complete sign-up. Please try again."));
        }
    }
}
