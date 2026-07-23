package com.travolish.traveller.kyc.controller;

import com.travolish.traveller.kyc.dto.HostKYCDTO;
import com.travolish.traveller.kyc.entity.HostKYC;
import com.travolish.traveller.kyc.repository.HostKYCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/kyc")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminKYCController {

    private final HostKYCRepository hostKYCRepository;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<List<HostKYCDTO>> getAllKYC(
            @RequestParam(required = false) String status) {
        List<HostKYC> records = status != null
                ? hostKYCRepository.findByKYCStatus(status)
                : hostKYCRepository.findAll();
        List<HostKYCDTO> dtos = records.stream()
                .map(this::toListDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<HostKYCDTO> getKYCById(@PathVariable Long id) {
        return hostKYCRepository.findById(id)
                .map(k -> modelMapper.map(k, HostKYCDTO.class))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<HostKYCDTO>> getPendingKYC() {
        List<HostKYCDTO> dtos = hostKYCRepository.findPendingVerifications().stream()
                .map(this::toListDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    private HostKYCDTO toListDto(HostKYC k) {
        HostKYCDTO dto = new HostKYCDTO();
        dto.setId(k.getId());
        dto.setHostId(k.getHostId());
        dto.setFirstName(k.getFirstName());
        dto.setLastName(k.getLastName());
        dto.setDateOfBirth(k.getDateOfBirth());
        dto.setPhoneNumber(k.getPhoneNumber());
        dto.setNationality(k.getNationality());
        dto.setNationalIdNumber(k.getNationalIdNumber());
        dto.setAddressLine1(k.getAddressLine1());
        dto.setAddressLine2(k.getAddressLine2());
        dto.setCity(k.getCity());
        dto.setStateProvince(k.getStateProvince());
        dto.setPostalCode(k.getPostalCode());
        dto.setCountry(k.getCountry());
        dto.setBusinessName(k.getBusinessName());
        dto.setBusinessType(k.getBusinessType());
        dto.setBusinessRegistrationNumber(k.getBusinessRegistrationNumber());
        dto.setTaxId(k.getTaxId());
        dto.setBusinessLicenseNumber(k.getBusinessLicenseNumber());
        dto.setKycStatus(k.getKycStatus());
        dto.setVerificationLevel(k.getVerificationLevel());
        dto.setVerificationDate(k.getVerificationDate());
        dto.setExpiryDate(k.getExpiryDate());
        dto.setRejectionReason(k.getRejectionReason());
        dto.setRejectionDate(k.getRejectionDate());
        dto.setNotes(k.getNotes());
        dto.setReviewerId(k.getReviewerId());
        dto.setRiskScore(k.getRiskScore());
        dto.setRiskLevel(k.getRiskLevel());
        dto.setCreatedAt(k.getCreatedAt());
        dto.setUpdatedAt(k.getUpdatedAt());
        // documents and bankAccounts deliberately omitted — not needed for list view
        return dto;
    }

    @Transactional
    @PostMapping("/{id}/approve")
    public ResponseEntity<HostKYCDTO> approveKYC(@PathVariable Long id) {
        return hostKYCRepository.findById(id).map(kyc -> {
            kyc.setKycStatus("VERIFIED");
            kyc.setVerificationDate(LocalDateTime.now());
            HostKYC saved = hostKYCRepository.save(kyc);
            log.info("KYC {} approved by admin", id);
            return ResponseEntity.ok(modelMapper.map(saved, HostKYCDTO.class));
        }).orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    @PostMapping("/{id}/reject")
    public ResponseEntity<HostKYCDTO> rejectKYC(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Admin decision") String reason) {
        return hostKYCRepository.findById(id).map(kyc -> {
            kyc.setKycStatus("REJECTED");
            kyc.setRejectionReason(reason);
            kyc.setRejectionDate(LocalDateTime.now());
            HostKYC saved = hostKYCRepository.save(kyc);
            log.info("KYC {} rejected by admin: {}", id, reason);
            return ResponseEntity.ok(modelMapper.map(saved, HostKYCDTO.class));
        }).orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    @PatchMapping("/{id}/assign")
    public ResponseEntity<HostKYCDTO> assignReviewer(
            @PathVariable Long id,
            @RequestParam Long reviewerId) {
        return hostKYCRepository.findById(id).map(kyc -> {
            kyc.setReviewerId(reviewerId);
            HostKYC saved = hostKYCRepository.save(kyc);
            log.info("KYC {} assigned to reviewer {}", id, reviewerId);
            return ResponseEntity.ok(modelMapper.map(saved, HostKYCDTO.class));
        }).orElse(ResponseEntity.notFound().build());
    }

    @Transactional
    @PostMapping("/{id}/request-resubmit")
    public ResponseEntity<HostKYCDTO> requestResubmit(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Additional documents required") String reason) {
        return hostKYCRepository.findById(id).map(kyc -> {
            kyc.setKycStatus("RESUBMIT_REQUESTED");
            kyc.setNotes(reason);
            HostKYC saved = hostKYCRepository.save(kyc);
            log.info("KYC {} resubmission requested: {}", id, reason);
            return ResponseEntity.ok(modelMapper.map(saved, HostKYCDTO.class));
        }).orElse(ResponseEntity.notFound().build());
    }
}
