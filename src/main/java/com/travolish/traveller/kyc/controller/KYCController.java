package com.travolish.traveller.kyc.controller;

import com.travolish.traveller.kyc.dto.*;
import com.travolish.traveller.kyc.service.KYCService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/host/kyc")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class KYCController {
    
    private final KYCService kycService;
    
    @Value("${app.signin.url:http://localhost:3000/signin}")
    private String signInUrl;
    
    /**
     * Submit initial KYC information
     * POST /api/host/kyc/submit
     */
    @PostMapping("/submit")
    public ResponseEntity<HostKYCDTO> submitKYC(
        @Valid @RequestBody SubmitKYCRequest request,
        Authentication authentication) {
        try {
            if (authentication == null) {
                log.error("Authentication is null for KYC submission request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            log.info("KYC submission request from user: {}", authentication.getName());
            
            // Extract host ID from authentication
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
     * Public KYC submission endpoint with redirect to sign-in page
     * POST /api/host/kyc/submit-redirect
     * No authentication required - creates temporary KYC record and redirects to sign-in
     */
    @PostMapping("/submit-redirect")
    public RedirectView submitKYCWithRedirect(@Valid @RequestBody SubmitKYCRequest request) {
        try {
            log.info("Public KYC submission request received - will redirect to sign-in");
            
            // Submit KYC for temporary user (will be linked to authenticated user later)
            // Using 0 or null as temporary hostId to indicate pending authentication
            HostKYCDTO kycDTO = kycService.submitKYCTemporary(request);
            
            log.info("Temporary KYC submitted successfully with ID: {}", kycDTO.getId());
            
            // Return redirect to sign-in page
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(signInUrl);
            
            return redirectView;
        } catch (IllegalStateException e) {
            log.warn("KYC submission failed: {}", e.getMessage());
            // Redirect to sign-in on error as well
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(signInUrl + "?error=kyc_submission_failed");
            return redirectView;
        } catch (Exception e) {
            log.error("Error submitting KYC", e);
            // Redirect to sign-in with error on exception
            RedirectView redirectView = new RedirectView();
            redirectView.setUrl(signInUrl + "?error=internal_error");
            return redirectView;
        }
    }
    
    /**
     * Get current KYC status
     * GET /api/host/kyc/status
     */
    @GetMapping("/status")
    public ResponseEntity<HostKYCDTO> getKYCStatus(Authentication authentication) {
        try {
            if (authentication == null) {
                log.error("Authentication is null for KYC status request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
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
            if (authentication == null) {
                log.error("Authentication is null for document upload request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
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
            if (authentication == null) {
                log.error("Authentication is null for get documents request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
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
            if (authentication == null) {
                log.error("Authentication is null for bank account registration request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
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
            if (authentication == null) {
                log.error("Authentication is null for get bank accounts request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
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
            if (authentication == null) {
                log.error("Authentication is null for confirm micro deposit request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
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
            if (authentication == null) {
                log.error("Authentication is null for set primary bank account request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
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
            if (authentication == null) {
                log.error("Authentication is null for verification status request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
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
            if (authentication == null) {
                log.error("Authentication is null for KYC profile request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
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
        if (authentication == null) {
            throw new IllegalStateException("Authentication is required for this operation");
        }
        
        try {
            // Extract from OAuth2 principal
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User) {
                org.springframework.security.oauth2.core.user.OAuth2User oAuth2User = 
                    (org.springframework.security.oauth2.core.user.OAuth2User) principal;
                
                // Try to get hostId from attributes
                Object hostIdAttr = oAuth2User.getAttribute("hostId");
                if (hostIdAttr instanceof Number) {
                    return ((Number) hostIdAttr).longValue();
                }
                
                // Try to get from custom attributes
                Object customAttr = oAuth2User.getAttribute("custom:hostId");
                if (customAttr instanceof Number) {
                    return ((Number) customAttr).longValue();
                }
                
                log.warn("hostId not found in OAuth2 attributes, falling back to placeholder");
            }
            
            // Fallback: Use placeholder - in production, implement proper JWT token extraction
            // This should be replaced with actual JWT token parsing to get the host ID
            log.warn("Using placeholder hostId - implement proper JWT token parsing in production");
            return 1L;
            
        } catch (Exception e) {
            log.error("Error extracting host ID from authentication", e);
            throw new RuntimeException("Failed to extract host ID from authentication", e);
        }
    }
}
