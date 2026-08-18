package com.travolish.traveller.kyc.controller;

import com.travolish.traveller.kyc.dto.*;
import com.travolish.traveller.kyc.service.GuestKYCService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/guest/kyc")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class GuestKYCController {

    private final GuestKYCService guestKYCService;

    // ── Submit / update KYC ───────────────────────────────────────────────────

    @PostMapping("/submit")
    public ResponseEntity<GuestKYCDTO> submitKYC(
            @RequestBody SubmitGuestKYCRequest request,
            @RequestParam(required = false) Long guestId,
            Authentication authentication) {
        try {
            Long resolved = resolveGuestId(authentication, guestId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.status(HttpStatus.CREATED).body(guestKYCService.submitKYC(resolved, request));
        } catch (Exception e) {
            log.error("Error submitting guest KYC", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Get status ────────────────────────────────────────────────────────────

    @GetMapping("/status")
    public ResponseEntity<GuestKYCDTO> getKYCStatus(
            @RequestParam(required = false) Long guestId,
            Authentication authentication) {
        try {
            Long resolved = resolveGuestId(authentication, guestId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.ok(guestKYCService.getKYCStatus(resolved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching guest KYC status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Get full profile ──────────────────────────────────────────────────────

    @GetMapping("/profile")
    public ResponseEntity<GuestKYCDTO> getKYCProfile(
            @RequestParam(required = false) Long guestId,
            Authentication authentication) {
        try {
            Long resolved = resolveGuestId(authentication, guestId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.ok(guestKYCService.getKYCProfile(resolved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching guest KYC profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Get verification status (progress) ────────────────────────────────────

    @GetMapping("/verification/status")
    public ResponseEntity<GuestVerificationStatusDTO> getVerificationStatus(
            @RequestParam(required = false) Long guestId,
            Authentication authentication) {
        try {
            Long resolved = resolveGuestId(authentication, guestId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.ok(guestKYCService.getVerificationStatus(resolved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching guest verification status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Upload document ───────────────────────────────────────────────────────

    @PostMapping("/document/upload")
    public ResponseEntity<GuestDocumentDTO> uploadDocument(
            @RequestBody GuestDocumentUploadRequest request,
            @RequestParam(required = false) Long guestId,
            Authentication authentication) {
        try {
            Long resolved = resolveGuestId(authentication, guestId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.status(HttpStatus.CREATED).body(guestKYCService.uploadDocument(resolved, request));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error uploading guest document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Get documents ─────────────────────────────────────────────────────────

    @GetMapping("/documents")
    public ResponseEntity<List<GuestDocumentDTO>> getDocuments(
            @RequestParam(required = false) Long guestId,
            Authentication authentication) {
        try {
            Long resolved = resolveGuestId(authentication, guestId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            return ResponseEntity.ok(guestKYCService.getDocuments(resolved));
        } catch (Exception e) {
            log.error("Error fetching guest documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Helper: resolve guestId from query param or JWT principal ─────────────

    private Long resolveGuestId(Authentication authentication, Long fallbackGuestId) {
        if (fallbackGuestId != null) return fallbackGuestId;
        if (authentication != null) {
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
                    Object id = oAuth2User.getAttribute("userId");
                    if (id instanceof Number n) return n.longValue();
                    id = oAuth2User.getAttribute("guestId");
                    if (id instanceof Number n) return n.longValue();
                }
            } catch (Exception e) {
                log.warn("Could not extract guestId from auth principal", e);
            }
        }
        return null;
    }
}
