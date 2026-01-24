package com.travolish.traveller.kyc.controller;

import com.travolish.traveller.kyc.dto.*;
import com.travolish.traveller.kyc.service.KYCService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/host/kyc")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class KYCController {
    
    private final KYCService kycService;
    
    /**
     * Submit initial KYC information
     * POST /api/host/kyc/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<HostKYCDTO> submitKYC(
        @Valid @RequestBody SubmitKYCRequest request,
        Authentication authentication) {
        try {
            log.info("KYC submission request from user: {}", authentication.getName());
            
            // Extract host ID from authentication (implementation depends on your auth setup)
            Long hostId = extractHostIdFromAuth(authentication);
            
            HostKYCDTO kycDTO = kycService.submitKYC(hostId, request);
            
            log.info("KYC submitted successfully for host: {}", hostId);
            return ResponseEntity.status(HttpStatus.CREATED).body(kycDTO);
        } catch (IllegalStateException e) {
            log.warn("KYC submission failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error submitting KYC", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get current KYC status
     * GET /api/host/kyc/status
     */
    @GetMapping("/status")
    public ResponseEntity<HostKYCDTO> getKYCStatus(Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Fetching KYC status for host: {}", hostId);
            
            HostKYCDTO kycDTO = kycService.getKYCStatus(hostId);
            return ResponseEntity.ok(kycDTO);
        } catch (IllegalArgumentException e) {
            log.warn("KYC not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching KYC status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Upload KYC document
     * POST /api/host/kyc/document/upload
     */
    @PostMapping("/document/upload")
    public ResponseEntity<HostDocumentDTO> uploadDocument(
        @Valid @RequestBody DocumentUploadRequest request,
        Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Document upload request for host: {}, type: {}", hostId, request.getDocumentType());
            
            HostDocumentDTO documentDTO = kycService.uploadDocument(hostId, request);
            
            log.info("Document uploaded successfully for host: {}", hostId);
            return ResponseEntity.status(HttpStatus.CREATED).body(documentDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Document upload failed - invalid input: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            log.warn("Document upload failed - state error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error uploading document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get list of documents for host
     * GET /api/host/kyc/documents
     */
    @GetMapping("/documents")
    public ResponseEntity<List<HostDocumentDTO>> getDocuments(Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Fetching documents for host: {}", hostId);
            
            List<HostDocumentDTO> documents = kycService.getDocuments(hostId);
            return ResponseEntity.ok(documents);
        } catch (Exception e) {
            log.error("Error fetching documents", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Register bank account for payouts
     * POST /api/host/kyc/bank/register
     */
    @PostMapping("/bank/register")
    public ResponseEntity<HostBankAccountDTO> registerBankAccount(
        @Valid @RequestBody BankAccountVerificationRequest request,
        Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Bank account registration for host: {}", hostId);
            
            HostBankAccountDTO bankAccountDTO = kycService.registerBankAccount(hostId, request);
            
            log.info("Bank account registered for host: {}", hostId);
            return ResponseEntity.status(HttpStatus.CREATED).body(bankAccountDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Bank account registration failed - invalid input: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            log.warn("Bank account registration failed - state error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error registering bank account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get bank accounts for host
     * GET /api/host/kyc/bank/accounts
     */
    @GetMapping("/bank/accounts")
    public ResponseEntity<List<HostBankAccountDTO>> getBankAccounts(Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Fetching bank accounts for host: {}", hostId);
            
            List<HostBankAccountDTO> bankAccounts = kycService.getBankAccounts(hostId);
            return ResponseEntity.ok(bankAccounts);
        } catch (Exception e) {
            log.error("Error fetching bank accounts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Confirm micro deposit amounts for bank account verification
     * POST /api/host/kyc/bank/{bankAccountId}/verify-micro-deposit
     */
    @PostMapping("/bank/{bankAccountId}/verify-micro-deposit")
    public ResponseEntity<HostBankAccountDTO> confirmMicroDeposit(
        @PathVariable Long bankAccountId,
        @RequestParam Double amount1,
        @RequestParam Double amount2,
        Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Confirming micro deposit for bank account: {} by host: {}", bankAccountId, hostId);
            
            HostBankAccountDTO bankAccountDTO = kycService.confirmMicroDeposit(bankAccountId, amount1, amount2);
            
            log.info("Micro deposit confirmed for bank account: {}", bankAccountId);
            return ResponseEntity.ok(bankAccountDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Micro deposit confirmation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            log.warn("Micro deposit confirmation failed - state error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error confirming micro deposit", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Set primary bank account for payouts
     * POST /api/host/kyc/bank/{bankAccountId}/set-primary
     */
    @PostMapping("/bank/{bankAccountId}/set-primary")
    public ResponseEntity<HostBankAccountDTO> setPrimaryBankAccount(
        @PathVariable Long bankAccountId,
        Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Setting primary bank account: {} for host: {}", bankAccountId, hostId);
            
            HostBankAccountDTO bankAccountDTO = kycService.setPrimaryBankAccount(hostId, bankAccountId);
            
            log.info("Primary bank account set: {}", bankAccountId);
            return ResponseEntity.ok(bankAccountDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to set primary bank account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            log.warn("Failed to set primary bank account - state error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error setting primary bank account", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get overall verification status for host
     * GET /api/host/verification/status
     */
    @GetMapping("/verification/status")
    public ResponseEntity<VerificationStatusDTO> getVerificationStatus(Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Fetching verification status for host: {}", hostId);
            
            VerificationStatusDTO status = kycService.getVerificationStatus(hostId);
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            log.warn("KYC not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching verification status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get complete KYC profile with all documents and bank accounts
     * GET /api/host/kyc/profile
     */
    @GetMapping("/profile")
    public ResponseEntity<HostKYCDTO> getKYCProfile(Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Fetching KYC profile for host: {}", hostId);
            
            HostKYCDTO kycProfile = kycService.getKYCProfile(hostId);
            return ResponseEntity.ok(kycProfile);
        } catch (IllegalArgumentException e) {
            log.warn("KYC profile not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching KYC profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Helper method to extract host ID from authentication
     * In a real implementation, this would extract from JWT token or session
     */
    private Long extractHostIdFromAuth(Authentication authentication) {
        // This is a placeholder implementation
        // In production, extract the actual host ID from the JWT token or session
        // For demonstration, parse from claims or custom principal
        // return userService.getUserIdByUsername(username);
        
        // Temporary implementation - in production, use actual user service
        return 1L; // Placeholder
    }
}
