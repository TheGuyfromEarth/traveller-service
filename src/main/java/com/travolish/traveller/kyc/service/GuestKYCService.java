package com.travolish.traveller.kyc.service;

import com.travolish.traveller.kyc.dto.*;
import com.travolish.traveller.kyc.entity.GuestDocument;
import com.travolish.traveller.kyc.entity.GuestKYC;
import com.travolish.traveller.kyc.repository.GuestDocumentRepository;
import com.travolish.traveller.kyc.repository.GuestKYCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuestKYCService {

    private final GuestKYCRepository guestKYCRepository;
    private final GuestDocumentRepository guestDocumentRepository;
    private final ModelMapper modelMapper;

    private static final List<String> REQUIRED_DOC_TYPES = List.of("GOVERNMENT_ID", "PROOF_OF_ADDRESS");

    // ── Submit KYC ────────────────────────────────────────────────────────────

    @Transactional
    public GuestKYCDTO submitKYC(Long guestId, SubmitGuestKYCRequest request) {
        log.info("Submitting KYC for guest: {}", guestId);

        var existing = guestKYCRepository.findByGuestId(guestId);
        if (existing.isPresent()) {
            // Update existing record instead of throwing — guest may resubmit to update info
            GuestKYC kyc = existing.get();
            applyPersonalInfo(kyc, request);
            assessRisk(kyc);
            kyc.setUpdatedAt(LocalDateTime.now());
            kyc = guestKYCRepository.save(kyc);
            log.info("KYC updated for guest: {}", guestId);
            return toDTO(kyc);
        }

        GuestKYC kyc = new GuestKYC();
        kyc.setGuestId(guestId);
        applyPersonalInfo(kyc, request);
        kyc.setKycStatus("PENDING");
        kyc.setVerificationLevel("LEVEL_0");
        assessRisk(kyc);
        kyc = guestKYCRepository.save(kyc);
        log.info("KYC created for guest: {}, id: {}", guestId, kyc.getId());
        return toDTO(kyc);
    }

    // ── Get KYC status ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public GuestKYCDTO getKYCStatus(Long guestId) {
        log.info("Fetching KYC status for guest: {}", guestId);
        GuestKYC kyc = guestKYCRepository.findByGuestId(guestId)
            .orElseThrow(() -> new IllegalArgumentException("KYC not found for guest: " + guestId));
        return toDTO(kyc);
    }

    // ── Get full KYC profile (includes documents) ──────────────────────────────

    @Transactional(readOnly = true)
    public GuestKYCDTO getKYCProfile(Long guestId) {
        log.info("Fetching KYC profile for guest: {}", guestId);
        GuestKYC kyc = guestKYCRepository.findByGuestId(guestId)
            .orElseThrow(() -> new IllegalArgumentException("KYC not found for guest: " + guestId));

        GuestKYCDTO dto = toDTO(kyc);
        List<GuestDocument> docs = guestDocumentRepository.findByGuestKYCId(kyc.getId());
        dto.setDocuments(docs.stream().map(this::toDocDTO).collect(Collectors.toList()));
        return dto;
    }

    // ── Get verification status (progress summary) ─────────────────────────────

    @Transactional(readOnly = true)
    public GuestVerificationStatusDTO getVerificationStatus(Long guestId) {
        log.info("Fetching verification status for guest: {}", guestId);
        GuestKYC kyc = guestKYCRepository.findByGuestId(guestId)
            .orElseThrow(() -> new IllegalArgumentException("KYC not found for guest: " + guestId));

        List<GuestDocument> docs = guestDocumentRepository.findByGuestKYCId(kyc.getId());
        int submitted = docs.size();
        int verified = (int) docs.stream()
            .filter(d -> "VERIFIED".equals(d.getVerificationStatus()))
            .count();
        int required = REQUIRED_DOC_TYPES.size(); // 2

        int progress = (verified * 100) / required;

        String overall;
        String ks = kyc.getKycStatus();
        if ("VERIFIED".equals(ks)) {
            overall = "VERIFIED";
        } else if ("REJECTED".equals(ks)) {
            overall = "REJECTED";
        } else if (verified >= required) {
            overall = "PENDING_FINAL_APPROVAL";
        } else if (submitted > 0) {
            overall = "UNDER_REVIEW";
        } else {
            overall = "NOT_STARTED";
        }

        GuestVerificationStatusDTO status = new GuestVerificationStatusDTO();
        status.setGuestId(guestId);
        status.setOverallStatus(overall);
        status.setKycStatus(ks);
        status.setVerificationLevel(kyc.getVerificationLevel());
        status.setDocumentsRequired(required);
        status.setDocumentsSubmitted(submitted);
        status.setDocumentsVerified(verified);
        status.setOverallProgress(progress);
        status.setRiskScore(kyc.getRiskScore());
        status.setRiskLevel(kyc.getRiskLevel());
        status.setRejectionReason(kyc.getRejectionReason());
        status.setNotes(kyc.getNotes());
        return status;
    }

    // ── Get documents ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GuestDocumentDTO> getDocuments(Long guestId) {
        var kycOpt = guestKYCRepository.findByGuestId(guestId);
        if (kycOpt.isEmpty()) return List.of();
        return guestDocumentRepository.findByGuestKYCId(kycOpt.get().getId())
            .stream()
            .map(this::toDocDTO)
            .collect(Collectors.toList());
    }

    // ── Upload document ────────────────────────────────────────────────────────

    @Transactional
    public GuestDocumentDTO uploadDocument(Long guestId, GuestDocumentUploadRequest request) {
        log.info("Uploading document for guest: {}, type: {}", guestId, request.getDocumentType());

        // Auto-create KYC record if none exists
        GuestKYC kyc = guestKYCRepository.findByGuestId(guestId).orElseGet(() -> {
            GuestKYC newKyc = new GuestKYC();
            newKyc.setGuestId(guestId);
            newKyc.setKycStatus("PENDING");
            newKyc.setVerificationLevel("LEVEL_0");
            assessRisk(newKyc);
            return guestKYCRepository.save(newKyc);
        });

        // Upsert: handle any duplicate docs of the same type gracefully
        List<GuestDocument> existing = guestDocumentRepository
            .findAllByGuestKYCIdAndDocumentType(kyc.getId(), request.getDocumentType());

        if (!existing.isEmpty() && "VERIFIED".equals(existing.get(0).getVerificationStatus())) {
            throw new IllegalStateException("A verified document of this type already exists and cannot be replaced");
        }

        // Remove older duplicates, keep only the most recent
        if (existing.size() > 1) {
            guestDocumentRepository.deleteAll(existing.subList(1, existing.size()));
        }

        GuestDocument doc = existing.isEmpty() ? null : existing.get(0);
        if (doc == null) {
            doc = new GuestDocument();
            doc.setGuestKYC(kyc);
            doc.setDocumentType(request.getDocumentType());
        }

        doc.setDocumentName(request.getDocumentName());
        doc.setFileUrl(request.getFileUrl() != null ? request.getFileUrl() : "pending-upload");
        doc.setIssuedDate(request.getIssuedDate() != null
            ? LocalDate.parse(request.getIssuedDate()) : LocalDate.now());
        doc.setExpiryDate(request.getExpiryDate() != null
            ? LocalDate.parse(request.getExpiryDate()) : LocalDate.now().plusYears(10));
        if (request.getDocumentNumber() != null) doc.setDocumentNumber(request.getDocumentNumber());
        if (request.getIssuingCountry() != null) doc.setIssuingCountry(request.getIssuingCountry());
        doc.setVerificationStatus("PENDING");
        doc.setUpdatedAt(LocalDateTime.now());

        doc = guestDocumentRepository.save(doc);

        // Move KYC to UNDER_REVIEW on first document
        if (guestDocumentRepository.countByGuestKYCId(kyc.getId()) == 1) {
            kyc.setKycStatus("UNDER_REVIEW");
            kyc.setVerificationLevel("LEVEL_1");
            kyc.setUpdatedAt(LocalDateTime.now());
            guestKYCRepository.save(kyc);
        }

        log.info("Document uploaded for guest: {}", guestId);
        return toDocDTO(doc);
    }

    // ── Admin: approve ─────────────────────────────────────────────────────────

    @Transactional
    public GuestKYCDTO approve(Long kycId) {
        GuestKYC kyc = guestKYCRepository.findById(kycId)
            .orElseThrow(() -> new IllegalArgumentException("Guest KYC not found: " + kycId));
        kyc.setKycStatus("VERIFIED");
        kyc.setVerificationDate(LocalDateTime.now());
        kyc.setVerificationLevel("LEVEL_2");
        kyc.setUpdatedAt(LocalDateTime.now());
        return toDTO(guestKYCRepository.save(kyc));
    }

    // ── Admin: reject ──────────────────────────────────────────────────────────

    @Transactional
    public GuestKYCDTO reject(Long kycId, String reason) {
        GuestKYC kyc = guestKYCRepository.findById(kycId)
            .orElseThrow(() -> new IllegalArgumentException("Guest KYC not found: " + kycId));
        kyc.setKycStatus("REJECTED");
        kyc.setRejectionReason(reason);
        kyc.setRejectionDate(LocalDateTime.now());
        kyc.setUpdatedAt(LocalDateTime.now());
        return toDTO(guestKYCRepository.save(kyc));
    }

    // ── Admin: request resubmission ────────────────────────────────────────────

    @Transactional
    public GuestKYCDTO requestResubmit(Long kycId, String reason) {
        GuestKYC kyc = guestKYCRepository.findById(kycId)
            .orElseThrow(() -> new IllegalArgumentException("Guest KYC not found: " + kycId));
        kyc.setKycStatus("RESUBMIT_REQUESTED");
        kyc.setNotes(reason);
        kyc.setUpdatedAt(LocalDateTime.now());
        return toDTO(guestKYCRepository.save(kyc));
    }

    // ── Admin: assign reviewer ─────────────────────────────────────────────────

    @Transactional
    public GuestKYCDTO assignReviewer(Long kycId, Long reviewerId) {
        GuestKYC kyc = guestKYCRepository.findById(kycId)
            .orElseThrow(() -> new IllegalArgumentException("Guest KYC not found: " + kycId));
        kyc.setReviewerId(reviewerId);
        kyc.setUpdatedAt(LocalDateTime.now());
        return toDTO(guestKYCRepository.save(kyc));
    }

    // ── Admin: list all ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<GuestKYCDTO> findAll(String status) {
        List<GuestKYC> records = status != null
            ? guestKYCRepository.findByKycStatus(status)
            : guestKYCRepository.findAll();
        return records.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GuestKYCDTO> findPending() {
        return guestKYCRepository.findPendingVerifications()
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GuestKYCDTO findById(Long kycId) {
        GuestKYC kyc = guestKYCRepository.findById(kycId)
            .orElseThrow(() -> new IllegalArgumentException("Guest KYC not found: " + kycId));
        GuestKYCDTO dto = toDTO(kyc);
        List<GuestDocument> docs = guestDocumentRepository.findByGuestKYCId(kycId);
        dto.setDocuments(docs.stream().map(this::toDocDTO).collect(Collectors.toList()));
        return dto;
    }

    // ── Risk scoring ───────────────────────────────────────────────────────────

    private void assessRisk(GuestKYC kyc) {
        int score = 0;

        if (kyc.getNationalIdNumber() == null || kyc.getNationalIdNumber().isBlank()) {
            score += 25;
        }

        if (kyc.getDateOfBirth() != null) {
            int age = LocalDate.now().getYear() - kyc.getDateOfBirth().getYear();
            if (age < 18) score += 30;
            if (age > 100) score += 20;
        } else {
            score += 20;
        }

        kyc.setRiskScore(score);
        kyc.setRiskLevel(score >= 75 ? "HIGH" : score >= 50 ? "MEDIUM" : "LOW");
        log.debug("Risk assessed for guest KYC — score: {}, level: {}", score, kyc.getRiskLevel());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void applyPersonalInfo(GuestKYC kyc, SubmitGuestKYCRequest req) {
        kyc.setFirstName(req.getFirstName());
        kyc.setLastName(req.getLastName());
        kyc.setDateOfBirth(req.getDateOfBirth());
        kyc.setPhoneNumber(req.getPhoneNumber());
        kyc.setNationality(req.getNationality());
        kyc.setNationalIdNumber(req.getNationalIdNumber());
        kyc.setAddressLine1(req.getAddressLine1());
        kyc.setAddressLine2(req.getAddressLine2());
        kyc.setCity(req.getCity());
        kyc.setStateProvince(req.getStateProvince());
        kyc.setPostalCode(req.getPostalCode());
        kyc.setCountry(req.getCountry());
    }

    private GuestKYCDTO toDTO(GuestKYC kyc) {
        return modelMapper.map(kyc, GuestKYCDTO.class);
    }

    private GuestDocumentDTO toDocDTO(GuestDocument doc) {
        return modelMapper.map(doc, GuestDocumentDTO.class);
    }
}
