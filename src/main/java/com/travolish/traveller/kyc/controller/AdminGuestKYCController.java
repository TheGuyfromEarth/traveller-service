package com.travolish.traveller.kyc.controller;

import com.travolish.traveller.kyc.dto.GuestKYCDTO;
import com.travolish.traveller.kyc.service.GuestKYCService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/kyc/guests")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminGuestKYCController {

    private final GuestKYCService guestKYCService;

    @GetMapping
    public ResponseEntity<List<GuestKYCDTO>> getAllGuestKYC(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(guestKYCService.findAll(status));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<GuestKYCDTO>> getPendingGuestKYC() {
        return ResponseEntity.ok(guestKYCService.findPending());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GuestKYCDTO> getGuestKYCById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(guestKYCService.findById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    @PostMapping("/{id}/approve")
    public ResponseEntity<GuestKYCDTO> approveGuestKYC(@PathVariable Long id) {
        try {
            GuestKYCDTO result = guestKYCService.approve(id);
            log.info("Guest KYC {} approved by admin", id);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    @PostMapping("/{id}/reject")
    public ResponseEntity<GuestKYCDTO> rejectGuestKYC(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Admin decision") String reason) {
        try {
            GuestKYCDTO result = guestKYCService.reject(id, reason);
            log.info("Guest KYC {} rejected by admin: {}", id, reason);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    @PatchMapping("/{id}/assign")
    public ResponseEntity<GuestKYCDTO> assignReviewer(
            @PathVariable Long id,
            @RequestParam Long reviewerId) {
        try {
            GuestKYCDTO result = guestKYCService.assignReviewer(id, reviewerId);
            log.info("Guest KYC {} assigned to reviewer {}", id, reviewerId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Transactional
    @PostMapping("/{id}/request-resubmit")
    public ResponseEntity<GuestKYCDTO> requestResubmit(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Additional documents required") String reason) {
        try {
            GuestKYCDTO result = guestKYCService.requestResubmit(id, reason);
            log.info("Guest KYC {} resubmission requested: {}", id, reason);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
