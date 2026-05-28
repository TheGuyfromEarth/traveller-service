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
                .map(k -> modelMapper.map(k, HostKYCDTO.class))
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/pending")
    public ResponseEntity<List<HostKYCDTO>> getPendingKYC() {
        List<HostKYCDTO> dtos = hostKYCRepository.findPendingVerifications().stream()
                .map(k -> modelMapper.map(k, HostKYCDTO.class))
                .toList();
        return ResponseEntity.ok(dtos);
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
