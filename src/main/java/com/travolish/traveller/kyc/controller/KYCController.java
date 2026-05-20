package com.travolish.traveller.kyc.controller;

import com.travolish.traveller.kyc.dto.*;
import com.travolish.traveller.kyc.service.KYCService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/host/kyc")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class KYCController {

    private final KYCService kycService;

    // ── Submit KYC (authenticated) ───────────────────────────────────────────
    @PostMapping("/submit")
    public ResponseEntity<HostKYCDTO> submitKYC(
            @RequestBody SubmitKYCRequest request,
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            HostKYCDTO dto = kycService.submitKYC(resolved, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error submitting KYC", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Submit KYC (no auth required — accepts hostId as query param) ────────
    @PostMapping("/submit-redirect")
    public ResponseEntity<HostKYCDTO> submitKYCPublic(
            @RequestBody SubmitKYCRequest request,
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) resolved = 0L;
            HostKYCDTO dto = kycService.submitKYCTemporary(resolved, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalStateException e) {
            // KYC already exists — return conflict so the caller knows
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error submitting KYC (public)", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Get KYC status ───────────────────────────────────────────────────────
    @GetMapping("/status")
    public ResponseEntity<HostKYCDTO> getKYCStatus(
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            HostKYCDTO dto = kycService.getKYCStatus(resolved);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching KYC status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Upload document ──────────────────────────────────────────────────────
    @PostMapping("/document/upload")
    public ResponseEntity<HostDocumentDTO> uploadDocument(
            @RequestBody DocumentUploadRequest request,
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            HostDocumentDTO dto = kycService.uploadDocument(resolved, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error uploading document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Get documents ────────────────────────────────────────────────────────
    @GetMapping("/documents")
    public ResponseEntity<List<HostDocumentDTO>> getDocuments(
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            List<HostDocumentDTO> docs = kycService.getDocuments(resolved);
            return ResponseEntity.ok(docs);
        } catch (Exception e) {
            log.error("Error fetching documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Register bank account ────────────────────────────────────────────────
    @PostMapping("/bank/register")
    public ResponseEntity<HostBankAccountDTO> registerBankAccount(
            @RequestBody BankAccountVerificationRequest request,
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            HostBankAccountDTO dto = kycService.registerBankAccount(resolved, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error registering bank account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Get bank accounts ────────────────────────────────────────────────────
    @GetMapping("/bank/accounts")
    public ResponseEntity<List<HostBankAccountDTO>> getBankAccounts(
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            List<HostBankAccountDTO> accounts = kycService.getBankAccounts(resolved);
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            log.error("Error fetching bank accounts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Verify micro deposit ─────────────────────────────────────────────────
    @PostMapping("/bank/{bankAccountId}/verify-micro-deposit")
    public ResponseEntity<HostBankAccountDTO> confirmMicroDeposit(
            @PathVariable Long bankAccountId,
            @RequestParam Double amount1,
            @RequestParam Double amount2,
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            HostBankAccountDTO dto = kycService.confirmMicroDeposit(bankAccountId, amount1, amount2);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error confirming micro deposit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Set primary bank account ─────────────────────────────────────────────
    @PostMapping("/bank/{bankAccountId}/set-primary")
    public ResponseEntity<HostBankAccountDTO> setPrimaryBankAccount(
            @PathVariable Long bankAccountId,
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            HostBankAccountDTO dto = kycService.setPrimaryBankAccount(resolved, bankAccountId);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error setting primary bank account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Overall verification status ──────────────────────────────────────────
    @GetMapping("/verification/status")
    public ResponseEntity<VerificationStatusDTO> getVerificationStatus(
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            VerificationStatusDTO status = kycService.getVerificationStatus(resolved);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching verification status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Full KYC profile ─────────────────────────────────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<HostKYCDTO> getKYCProfile(
            @RequestParam(required = false) Long hostId,
            Authentication authentication) {
        try {
            Long resolved = resolveHostId(authentication, hostId);
            if (resolved == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            HostKYCDTO dto = kycService.getKYCProfile(resolved);
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching KYC profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private Long resolveHostId(Authentication authentication, Long fallbackHostId) {
        if (fallbackHostId != null) return fallbackHostId;
        if (authentication != null) {
            try {
                Object principal = authentication.getPrincipal();
                if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
                    Object hostIdAttr = oAuth2User.getAttribute("hostId");
                    if (hostIdAttr instanceof Number n) return n.longValue();
                    Object customAttr = oAuth2User.getAttribute("custom:hostId");
                    if (customAttr instanceof Number n) return n.longValue();
                }
            } catch (Exception e) {
                log.warn("Could not extract hostId from auth principal", e);
            }
        }
        return null;
    }
}
