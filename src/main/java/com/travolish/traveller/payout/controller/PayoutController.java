package com.travolish.traveller.payout.controller;

import com.travolish.traveller.payout.dto.*;
import com.travolish.traveller.payout.service.PayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payouts")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PayoutController {
    
    private final PayoutService payoutService;
    
    /**
     * Request a payout
     * POST /api/payouts/request
     */
    @PostMapping("/request")
    public ResponseEntity<PayoutDTO> requestPayout(
        @Valid @RequestBody PayoutRequest request,
        Authentication authentication) {
        try {
            log.info("Payout request from user: {}", authentication.getName());
            
            Long hostId = extractHostIdFromAuth(authentication);
            PayoutDTO payoutDTO = payoutService.requestPayout(hostId, request);
            
            log.info("Payout requested successfully: {}", payoutDTO.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(payoutDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payout request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error requesting payout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get payout status
     * GET /api/payouts/{id}/status
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<PayoutStatusDTO> getPayoutStatus(
        @PathVariable Long id,
        Authentication authentication) {
        try {
            log.info("Fetching payout status: {}", id);
            
            PayoutStatusDTO statusDTO = payoutService.getPayoutStatus(id);
            return ResponseEntity.ok(statusDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Payout not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching payout status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get payout history (paginated)
     * GET /api/payouts/history
     */
    @GetMapping("/history")
    public ResponseEntity<Page<PayoutHistoryDTO>> getPayoutHistory(
        Pageable pageable,
        Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Fetching payout history for host: {}", hostId);
            
            Page<PayoutHistoryDTO> history = payoutService.getPayoutHistory(hostId, pageable);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching payout history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get payout history by status
     * GET /api/payouts/history/status/{status}
     */
    @GetMapping("/history/status/{status}")
    public ResponseEntity<Page<PayoutHistoryDTO>> getPayoutHistoryByStatus(
        @PathVariable String status,
        Pageable pageable,
        Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Fetching {} payouts for host: {}", status, hostId);
            
            Page<PayoutHistoryDTO> history = payoutService.getPayoutHistoryByStatus(hostId, status, pageable);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching payout history by status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get payout balance
     * GET /api/payouts/balance
     */
    @GetMapping("/balance")
    public ResponseEntity<PayoutBalanceDTO> getBalance(Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Fetching balance for host: {}", hostId);
            
            PayoutBalanceDTO balanceDTO = payoutService.getHostBalance(hostId);
            return ResponseEntity.ok(balanceDTO);
        } catch (Exception e) {
            log.error("Error fetching balance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get payouts by date range
     * GET /api/payouts/date-range
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<PayoutDTO>> getPayoutsByDateRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        Authentication authentication) {
        try {
            Long hostId = extractHostIdFromAuth(authentication);
            log.info("Fetching payouts for host: {} from {} to {}", hostId, startDate, endDate);
            
            List<PayoutDTO> payouts = payoutService.getPayoutsByDateRange(hostId, startDate, endDate);
            return ResponseEntity.ok(payouts);
        } catch (Exception e) {
            log.error("Error fetching payouts by date range", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Admin: Approve a payout
     * POST /api/payouts/{id}/approve
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<PayoutDTO> approvePayout(
        @PathVariable Long id,
        @RequestParam(required = false) String notes,
        Authentication authentication) {
        try {
            log.info("Approving payout: {}", id);
            
            PayoutDTO payoutDTO = payoutService.approvePayout(id, notes);
            return ResponseEntity.ok(payoutDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Payout not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Cannot approve payout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error approving payout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Admin: Process a payout
     * POST /api/payouts/{id}/process
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<PayoutDTO> processPayout(
        @PathVariable Long id,
        @RequestParam String paymentMethod,
        Authentication authentication) {
        try {
            log.info("Processing payout: {} with method: {}", id, paymentMethod);
            
            PayoutDTO payoutDTO = payoutService.processPayout(id, paymentMethod);
            return ResponseEntity.ok(payoutDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Payout not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Cannot process payout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error processing payout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Admin: Complete a payout
     * POST /api/payouts/{id}/complete
     */
    @PostMapping("/{id}/complete")
    public ResponseEntity<PayoutDTO> completePayout(
        @PathVariable Long id,
        @RequestParam String transactionReference,
        Authentication authentication) {
        try {
            log.info("Completing payout: {}", id);
            
            PayoutDTO payoutDTO = payoutService.completePayout(id, transactionReference);
            return ResponseEntity.ok(payoutDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Payout not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error completing payout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Admin: Cancel a payout
     * POST /api/payouts/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<PayoutDTO> cancelPayout(
        @PathVariable Long id,
        @RequestParam(required = false) String reason,
        Authentication authentication) {
        try {
            log.info("Cancelling payout: {}", id);
            
            PayoutDTO payoutDTO = payoutService.cancelPayout(id, reason);
            return ResponseEntity.ok(payoutDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Payout not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Cannot cancel payout: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error cancelling payout", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Admin: Get pending payouts
     * GET /api/payouts/admin/pending
     */
    @GetMapping("/admin/pending")
    public ResponseEntity<List<PayoutDTO>> getPendingPayouts(Authentication authentication) {
        try {
            log.info("Fetching pending payouts for admin");
            
            List<PayoutDTO> payouts = payoutService.getPendingPayoutsForApproval();
            return ResponseEntity.ok(payouts);
        } catch (Exception e) {
            log.error("Error fetching pending payouts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Helper method to extract host ID from authentication
     */
    private Long extractHostIdFromAuth(Authentication authentication) {
        // Placeholder - in production, extract from JWT token or session
        return 1L;
    }
}
