package com.travolish.traveller.kyc.service;

import com.travolish.traveller.kyc.dto.*;
import com.travolish.traveller.kyc.entity.HostBankAccount;
import com.travolish.traveller.kyc.entity.HostDocument;
import com.travolish.traveller.kyc.entity.HostKYC;
import com.travolish.traveller.kyc.repository.HostBankAccountRepository;
import com.travolish.traveller.kyc.repository.HostDocumentRepository;
import com.travolish.traveller.kyc.repository.HostKYCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KYCService {
    
    private final HostKYCRepository hostKYCRepository;
    private final HostDocumentRepository hostDocumentRepository;
    private final HostBankAccountRepository hostBankAccountRepository;
    private final ModelMapper modelMapper;
    
    private static final double MICRO_DEPOSIT_MIN = 0.01;
    private static final double MICRO_DEPOSIT_MAX = 0.99;
    private static final int MICRO_DEPOSIT_VALIDITY_DAYS = 10;
    
    /**
     * Submit initial KYC information for a host
     */
    @Transactional
    public HostKYCDTO submitKYC(Long hostId, SubmitKYCRequest request) {
        log.info("Submitting KYC for host: {}", hostId);
        
        // Check if KYC already exists
        var existingKYC = hostKYCRepository.findByHostId(hostId);
        if (existingKYC.isPresent()) {
            throw new IllegalStateException("KYC already submitted for this host");
        }
        
        HostKYC hostKYC = new HostKYC();
        hostKYC.setHostId(hostId);
        hostKYC.setFirstName(request.getFirstName());
        hostKYC.setLastName(request.getLastName());
        hostKYC.setDateOfBirth(request.getDateOfBirth());
        hostKYC.setNationality(request.getNationality());
        hostKYC.setNationalIdNumber(request.getNationalIdNumber());
        
        // Address information
        hostKYC.setAddressLine1(request.getAddressLine1());
        hostKYC.setAddressLine2(request.getAddressLine2());
        hostKYC.setCity(request.getCity());
        hostKYC.setStateProvince(request.getStateProvince());
        hostKYC.setPostalCode(request.getPostalCode());
        hostKYC.setCountry(request.getCountry());
        
        // Business information
        hostKYC.setBusinessName(request.getBusinessName());
        hostKYC.setBusinessType(request.getBusinessType());
        hostKYC.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
        hostKYC.setTaxId(request.getTaxId());
        
        // Set initial status
        hostKYC.setKycStatus("PENDING");
        hostKYC.setVerificationLevel("LEVEL_0");
        hostKYC.setCreatedAt(LocalDateTime.now());
        hostKYC.setUpdatedAt(LocalDateTime.now());
        
        // Assess initial risk based on provided information
        assessRisk(hostKYC);
        
        hostKYC = hostKYCRepository.save(hostKYC);
        log.info("KYC submitted successfully for host: {} with ID: {}", hostId, hostKYC.getId());
        
        return modelMapper.map(hostKYC, HostKYCDTO.class);
    }
    
    /**
     * Submit KYC information temporarily without requiring authentication
     * This method saves KYC data with a temporary status, to be linked to a user after they sign in
     */
    @Transactional
    public HostKYCDTO submitKYCTemporary(Long hostId, SubmitKYCRequest request) {
        log.info("Submitting temporary KYC for host: {}", hostId);

        // Return existing record if one already exists for this host
        var existing = hostKYCRepository.findByHostId(hostId);
        if (existing.isPresent()) {
            log.info("KYC already exists for host: {}, returning existing record", hostId);
            return modelMapper.map(existing.get(), HostKYCDTO.class);
        }

        HostKYC hostKYC = new HostKYC();
        hostKYC.setHostId(hostId);
        hostKYC.setFirstName(request.getFirstName());
        hostKYC.setLastName(request.getLastName());
        hostKYC.setDateOfBirth(request.getDateOfBirth());
        hostKYC.setNationality(request.getNationality());
        hostKYC.setNationalIdNumber(request.getNationalIdNumber());
        
        // Address information
        hostKYC.setAddressLine1(request.getAddressLine1());
        hostKYC.setAddressLine2(request.getAddressLine2());
        hostKYC.setCity(request.getCity());
        hostKYC.setStateProvince(request.getStateProvince());
        hostKYC.setPostalCode(request.getPostalCode());
        hostKYC.setCountry(request.getCountry());
        
        // Business information
        hostKYC.setBusinessName(request.getBusinessName());
        hostKYC.setBusinessType(request.getBusinessType());
        hostKYC.setBusinessRegistrationNumber(request.getBusinessRegistrationNumber());
        hostKYC.setTaxId(request.getTaxId());
        
        // Set initial status - mark as TEMPORARY_SUBMITTED to indicate pending authentication
        hostKYC.setKycStatus("TEMPORARY_SUBMITTED");
        hostKYC.setVerificationLevel("LEVEL_0");
        hostKYC.setCreatedAt(LocalDateTime.now());
        hostKYC.setUpdatedAt(LocalDateTime.now());
        
        // Assess initial risk based on provided information
        assessRisk(hostKYC);
        
        hostKYC = hostKYCRepository.save(hostKYC);
        log.info("Temporary KYC submitted successfully with ID: {}", hostKYC.getId());
        
        return modelMapper.map(hostKYC, HostKYCDTO.class);
    }
    
    /**
     * Get KYC status for a specific host
     */
    @Transactional(readOnly = true)
    public HostKYCDTO getKYCStatus(Long hostId) {
        log.info("Fetching KYC status for host: {}", hostId);
        var hostKYC = hostKYCRepository.findByHostId(hostId)
            .orElseThrow(() -> new IllegalArgumentException("KYC not found for host: " + hostId));
        return modelMapper.map(hostKYC, HostKYCDTO.class);
    }
    
    /**
     * Upload a document for KYC verification
     */
    @Transactional
    public HostDocumentDTO uploadDocument(Long hostId, DocumentUploadRequest request) {
        log.info("Uploading document for host: {}, type: {}", hostId, request.getDocumentType());

        // Auto-create KYC record if it doesn't exist yet
        var hostKYC = hostKYCRepository.findByHostId(hostId).orElseGet(() -> {
            HostKYC newKyc = new HostKYC();
            newKyc.setHostId(hostId);
            newKyc.setKycStatus("PENDING");
            newKyc.setVerificationLevel("LEVEL_0");
            newKyc.setCreatedAt(LocalDateTime.now());
            newKyc.setUpdatedAt(LocalDateTime.now());
            assessRisk(newKyc);
            return hostKYCRepository.save(newKyc);
        });
        
        // Check if document type already exists and verified
        var existingDocument = hostDocumentRepository
            .findByHostKYCIdAndDocumentType(hostKYC.getId(), request.getDocumentType());
        
        if (existingDocument.isPresent() && "VERIFIED".equals(existingDocument.get().getVerificationStatus())) {
            throw new IllegalStateException("A verified document of this type already exists");
        }
        
        HostDocument document = new HostDocument();
        document.setHostKYC(hostKYC);
        document.setDocumentType(request.getDocumentType());
        document.setDocumentNumber(request.getDocumentNumber());
        document.setFileUrl(request.getFileUrl() != null ? request.getFileUrl() : "pending-upload");
        document.setIssuedDate(request.getIssuedDate() != null ? LocalDate.parse(request.getIssuedDate()) : LocalDate.now());
        document.setExpiryDate(request.getExpiryDate() != null ? LocalDate.parse(request.getExpiryDate()) : LocalDate.now().plusYears(10));
        
        // Set initial verification status
        document.setVerificationStatus("PENDING");
        document.setAiVerified(false);
        document.setAiConfidenceScore(0.0);
        
        document.setCreatedAt(LocalDateTime.now());
        
        document = hostDocumentRepository.save(document);
        
        // Update KYC status if this is first document
        if (hostDocumentRepository.countByHostKYCId(hostKYC.getId()) == 1) {
            hostKYC.setKycStatus("UNDER_REVIEW");
            hostKYC.setVerificationLevel("LEVEL_1");
            hostKYC.setUpdatedAt(LocalDateTime.now());
            hostKYCRepository.save(hostKYC);
        }
        
        log.info("Document uploaded successfully for host: {}", hostId);
        return modelMapper.map(document, HostDocumentDTO.class);
    }
    
    /**
     * Verify a document (admin operation)
     */
    @Transactional
    public HostDocumentDTO verifyDocument(Long documentId, String verificationNotes) {
        log.info("Verifying document: {}", documentId);
        
        var document = hostDocumentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        // Check if document is not expired
        if (document.getExpiryDate() != null && document.getExpiryDate().isBefore(LocalDate.now())) {
            document.setVerificationStatus("EXPIRED");
            hostDocumentRepository.save(document);
            throw new IllegalStateException("Document has expired");
        }
        
        document.setVerificationStatus("VERIFIED");
        document.setVerifiedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        
        document = hostDocumentRepository.save(document);
        
        // Update KYC verification level if all required documents are verified
        updateKYCVerificationLevel(document.getHostKYC().getId());
        
        log.info("Document verified successfully: {}", documentId);
        return modelMapper.map(document, HostDocumentDTO.class);
    }
    
    /**
     * Reject a document (admin operation)
     */
    @Transactional
    public HostDocumentDTO rejectDocument(Long documentId, String rejectionReason) {
        log.info("Rejecting document: {}", documentId);
        
        var document = hostDocumentRepository.findById(documentId)
            .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));
        
        document.setVerificationStatus("REJECTED");
        document.setUpdatedAt(LocalDateTime.now());
        
        document = hostDocumentRepository.save(document);
        log.info("Document rejected: {}", documentId);
        
        return modelMapper.map(document, HostDocumentDTO.class);
    }
    
    /**
     * Register bank account for a host
     */
    @Transactional
    public HostBankAccountDTO registerBankAccount(Long hostId, BankAccountVerificationRequest request) {
        log.info("Registering bank account for host: {}", hostId);

        // Auto-create KYC record if it doesn't exist yet
        var hostKYC = hostKYCRepository.findByHostId(hostId).orElseGet(() -> {
            HostKYC newKyc = new HostKYC();
            newKyc.setHostId(hostId);
            newKyc.setKycStatus("PENDING");
            newKyc.setVerificationLevel("LEVEL_0");
            newKyc.setCreatedAt(LocalDateTime.now());
            newKyc.setUpdatedAt(LocalDateTime.now());
            assessRisk(newKyc);
            return hostKYCRepository.save(newKyc);
        });
        
        // Check for duplicate account
        var existingAccount = hostBankAccountRepository
            .findByHostKYCIdAndAccountNumber(hostKYC.getId(), request.getAccountNumber());
        
        if (existingAccount.isPresent()) {
            throw new IllegalStateException("This bank account is already registered");
        }
        
        HostBankAccount bankAccount = new HostBankAccount();
        bankAccount.setHostKYC(hostKYC);
        bankAccount.setBankName(request.getBankName() != null ? request.getBankName() : "Unknown Bank");
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setAccountHolderName(request.getAccountHolderName() != null ? request.getAccountHolderName() : "Account Holder");
        bankAccount.setAccountType(request.getAccountType() != null ? request.getAccountType() : "SAVINGS");
        bankAccount.setSwiftCode(request.getSwiftCode());
        bankAccount.setIban(request.getIban());
        bankAccount.setRoutingNumber(request.getRoutingNumber());
        bankAccount.setCountry(request.getCountry() != null ? request.getCountry() : "IN");
        bankAccount.setCurrency(request.getCurrency() != null ? request.getCurrency() : "INR");

        // Set verification method
        bankAccount.setVerificationMethod(request.getVerificationMethod());

        // Initialize verification status
        if ("DOCUMENT".equals(request.getVerificationMethod())) {
            bankAccount.setDocumentVerified(false);
            bankAccount.setVerificationStatus("PENDING");
            bankAccount.setBankStatementUrl(request.getBankStatementUrl());
        } else if ("MICRO_DEPOSIT".equals(request.getVerificationMethod())) {
            generateMicroDeposits(bankAccount);
            bankAccount.setVerificationStatus("AWAITING_CONFIRMATION");
        } else {
            bankAccount.setVerificationStatus("PENDING");
        }

        bankAccount.setIsPrimary(false);
        bankAccount.setIsActive(true);
        bankAccount.setCreatedAt(LocalDateTime.now());
        bankAccount.setUpdatedAt(LocalDateTime.now());
        
        bankAccount = hostBankAccountRepository.save(bankAccount);
        log.info("Bank account registered for host: {}", hostId);
        
        return modelMapper.map(bankAccount, HostBankAccountDTO.class);
    }
    
    /**
     * Confirm micro deposit amounts to verify bank account
     */
    @Transactional
    public HostBankAccountDTO confirmMicroDeposit(Long bankAccountId, Double amount1, Double amount2) {
        log.info("Confirming micro deposit for bank account: {}", bankAccountId);
        
        var bankAccount = hostBankAccountRepository.findById(bankAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + bankAccountId));
        
        // Check if micro deposit is still valid
        if (bankAccount.getMicroDepositAmount1() == null ||
            bankAccount.getMicroDepositAmount2() == null) {
            throw new IllegalStateException("Micro deposits not generated for this account");
        }
        
        // Validate micro deposit amounts
        if (!bankAccount.getMicroDepositAmount1().equals(amount1) ||
            !bankAccount.getMicroDepositAmount2().equals(amount2)) {
            throw new IllegalArgumentException("Micro deposit amounts do not match");
        }
        
        bankAccount.setVerificationStatus("VERIFIED");
        bankAccount.setVerifiedAt(LocalDateTime.now());
        bankAccount.setUpdatedAt(LocalDateTime.now());
        
        bankAccount = hostBankAccountRepository.save(bankAccount);
        log.info("Bank account verified via micro deposit: {}", bankAccountId);
        
        return modelMapper.map(bankAccount, HostBankAccountDTO.class);
    }
    
    /**
     * Verify bank account via document (admin operation)
     */
    @Transactional
    public HostBankAccountDTO verifyBankAccountDocument(Long bankAccountId, String verificationNotes) {
        log.info("Verifying bank account via document: {}", bankAccountId);
        
        var bankAccount = hostBankAccountRepository.findById(bankAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + bankAccountId));
        
        bankAccount.setDocumentVerified(true);
        bankAccount.setVerificationStatus("VERIFIED");
        bankAccount.setVerifiedAt(LocalDateTime.now());
        bankAccount.setUpdatedAt(LocalDateTime.now());
        
        bankAccount = hostBankAccountRepository.save(bankAccount);
        log.info("Bank account verified via document: {}", bankAccountId);
        
        return modelMapper.map(bankAccount, HostBankAccountDTO.class);
    }
    
    /**
     * Set a bank account as primary for payouts
     */
    @Transactional
    public HostBankAccountDTO setPrimaryBankAccount(Long hostId, Long bankAccountId) {
        log.info("Setting primary bank account for host: {}", hostId);
        
        var hostKYC = hostKYCRepository.findByHostId(hostId)
            .orElseThrow(() -> new IllegalArgumentException("KYC not found for host: " + hostId));
        
        var newPrimaryAccount = hostBankAccountRepository.findById(bankAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Bank account not found: " + bankAccountId));
        
        if (!newPrimaryAccount.getHostKYC().getId().equals(hostKYC.getId())) {
            throw new IllegalArgumentException("Bank account does not belong to this host");
        }
        
        if (!"VERIFIED".equals(newPrimaryAccount.getVerificationStatus())) {
            throw new IllegalStateException("Only verified bank accounts can be set as primary");
        }
        
        // Remove primary flag from existing primary account
        var currentPrimary = hostBankAccountRepository.findPrimaryBankAccount(hostKYC.getId());
        if (currentPrimary.isPresent()) {
            currentPrimary.get().setIsPrimary(false);
            hostBankAccountRepository.save(currentPrimary.get());
        }
        
        // Set new primary account
        newPrimaryAccount.setIsPrimary(true);
        newPrimaryAccount = hostBankAccountRepository.save(newPrimaryAccount);
        
        log.info("Primary bank account set for host: {}", hostId);
        return modelMapper.map(newPrimaryAccount, HostBankAccountDTO.class);
    }
    
    /**
     * Get complete verification status for a host
     */
    @Transactional(readOnly = true)
    public VerificationStatusDTO getVerificationStatus(Long hostId) {
        log.info("Fetching verification status for host: {}", hostId);
        
        var hostKYC = hostKYCRepository.findByHostId(hostId)
            .orElseThrow(() -> new IllegalArgumentException("KYC not found for host: " + hostId));
        
        // Document verification status
        List<HostDocument> allDocuments = hostDocumentRepository.findByHostKYCId(hostKYC.getId());
        int documentsSubmitted = allDocuments.size();
        int documentsVerified = (int) allDocuments.stream()
            .filter(d -> "VERIFIED".equals(d.getVerificationStatus()))
            .count();
        
        var requiredDocuments = List.of("PASSPORT", "PROOF_OF_ADDRESS");
        int documentsRequired = requiredDocuments.size();
        
        // Bank account verification status
        List<HostBankAccount> bankAccounts = hostBankAccountRepository.findByHostKYCId(hostKYC.getId());
        int bankAccountsSubmitted = bankAccounts.size();
        int bankAccountsVerified = (int) bankAccounts.stream()
            .filter(b -> "VERIFIED".equals(b.getVerificationStatus()))
            .count();
        
        VerificationStatusDTO status = new VerificationStatusDTO();
        status.setHostId(hostId);
        status.setKycStatus(hostKYC.getKycStatus());
        status.setVerificationLevel(hostKYC.getVerificationLevel());
        status.setRiskLevel(hostKYC.getRiskLevel());
        status.setRiskScore(hostKYC.getRiskScore());
        
        status.setDocumentsSubmitted(documentsSubmitted);
        status.setDocumentsVerified(documentsVerified);
        status.setDocumentsRequired(documentsRequired);
        
        status.setBankAccountsSubmitted(bankAccountsSubmitted);
        status.setBankAccountsVerified(bankAccountsVerified);
        
        // Overall verification progress
        int totalRequirements = 2 + 1; // 2 documents + 1 bank account
        int completedRequirements = documentsVerified + 
                                    (bankAccountsVerified > 0 ? 1 : 0);
        status.setOverallProgress((completedRequirements * 100) / totalRequirements);
        
        // Determine overall status
        if ("VERIFIED".equals(hostKYC.getKycStatus())) {
            status.setOverallStatus("VERIFIED");
        } else if ("REJECTED".equals(hostKYC.getKycStatus())) {
            status.setOverallStatus("REJECTED");
        } else if (completedRequirements == totalRequirements) {
            status.setOverallStatus("PENDING_FINAL_APPROVAL");
        } else if (documentsSubmitted > 0 || bankAccountsSubmitted > 0) {
            status.setOverallStatus("UNDER_REVIEW");
        } else {
            status.setOverallStatus("NOT_STARTED");
        }
        
        return status;
    }
    
    /**
     * Get full KYC profile with all documents and bank accounts
     */
    @Transactional(readOnly = true)
    public HostKYCDTO getKYCProfile(Long hostId) {
        log.info("Fetching KYC profile for host: {}", hostId);
        
        var hostKYC = hostKYCRepository.findByHostId(hostId)
            .orElseThrow(() -> new IllegalArgumentException("KYC not found for host: " + hostId));
        
        var kycDTO = modelMapper.map(hostKYC, HostKYCDTO.class);
        
        return kycDTO;
    }
    
    /**
     * Get list of documents for a host
     */
    @Transactional(readOnly = true)
    public List<HostDocumentDTO> getDocuments(Long hostId) {
        var hostKYCOpt = hostKYCRepository.findByHostId(hostId);
        if (hostKYCOpt.isEmpty()) return List.of();
        return hostDocumentRepository.findByHostKYCId(hostKYCOpt.get().getId())
            .stream()
            .map(d -> modelMapper.map(d, HostDocumentDTO.class))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HostBankAccountDTO> getBankAccounts(Long hostId) {
        var hostKYCOpt = hostKYCRepository.findByHostId(hostId);
        if (hostKYCOpt.isEmpty()) return List.of();
        return hostBankAccountRepository.findByHostKYCId(hostKYCOpt.get().getId())
            .stream()
            .map(b -> modelMapper.map(b, HostBankAccountDTO.class))
            .collect(Collectors.toList());
    }
    
    /**
     * Assess risk for KYC based on information provided
     */
    private void assessRisk(HostKYC hostKYC) {
        double riskScore = 0.0;
        
        // Risk factors
        if (hostKYC.getDateOfBirth() != null) {
            LocalDate dob = hostKYC.getDateOfBirth();
            int age = LocalDate.now().getYear() - dob.getYear();
            if (age < 25) riskScore += 10;
            if (age > 80) riskScore += 15;
        } else {
            riskScore += 20;
        }
        
        if (hostKYC.getNationalIdNumber() == null || hostKYC.getNationalIdNumber().isEmpty()) {
            riskScore += 25;
        }
        
        if (hostKYC.getBusinessName() == null || hostKYC.getBusinessName().isEmpty()) {
            riskScore += 15;
        }
        
        if (hostKYC.getTaxId() == null || hostKYC.getTaxId().isEmpty()) {
            riskScore += 20;
        }
        
        // Determine risk level
        String riskLevel;
        if (riskScore >= 75) {
            riskLevel = "HIGH";
        } else if (riskScore >= 50) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "LOW";
        }
        
        hostKYC.setRiskScore((int) riskScore);
        hostKYC.setRiskLevel(riskLevel);
        
        log.debug("Risk assessment for KYC - Score: {}, Level: {}", riskScore, riskLevel);
    }
    
    /**
     * Update KYC verification level based on document verification progress
     */
    private void updateKYCVerificationLevel(Long hostKYCId) {
        var hostKYC = hostKYCRepository.findById(hostKYCId)
            .orElseThrow();
        
        var documents = hostDocumentRepository.findByHostKYCId(hostKYCId);
        var verifiedCount = documents.stream()
            .filter(d -> "VERIFIED".equals(d.getVerificationStatus()))
            .count();
        
        if (verifiedCount >= 2) {
            hostKYC.setVerificationLevel("LEVEL_2");
        } else if (verifiedCount >= 1) {
            hostKYC.setVerificationLevel("LEVEL_1");
        }
        
        hostKYC.setUpdatedAt(LocalDateTime.now());
        hostKYCRepository.save(hostKYC);
    }
    
    /**
     * Generate random micro deposit amounts for bank account verification
     */
    private void generateMicroDeposits(HostBankAccount bankAccount) {
        Random random = new Random();
        double amount1 = Math.round((MICRO_DEPOSIT_MIN + 
            (MICRO_DEPOSIT_MAX - MICRO_DEPOSIT_MIN) * random.nextDouble()) * 100.0) / 100.0;
        double amount2 = Math.round((MICRO_DEPOSIT_MIN + 
            (MICRO_DEPOSIT_MAX - MICRO_DEPOSIT_MIN) * random.nextDouble()) * 100.0) / 100.0;
        
        bankAccount.setMicroDepositAmount1(amount1);
        bankAccount.setMicroDepositAmount2(amount2);
        bankAccount.setMicroDepositValidUntil(
            LocalDateTime.now().plusDays(MICRO_DEPOSIT_VALIDITY_DAYS)
        );
        
        log.debug("Micro deposit amounts generated for bank account verification");
    }
}
