package com.travolish.traveller.payout.service;

import com.travolish.traveller.payout.dto.*;
import com.travolish.traveller.payout.entity.Payout;
import com.travolish.traveller.payout.repository.PayoutRepository;
import com.travolish.traveller.config.PlatformEconomicsProperties;
import com.travolish.traveller.kyc.entity.HostBankAccount;
import com.travolish.traveller.kyc.repository.HostBankAccountRepository;
import com.travolish.traveller.payment.service.RazorpayIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayoutService {
    
    private final PayoutRepository payoutRepository;
    private final HostBankAccountRepository hostBankAccountRepository;
    private final HostLedgerService hostLedgerService;
    private final RazorpayIntegrationService razorpayIntegrationService;
    private final PlatformEconomicsProperties economicsProperties;
    private final RazorpayXService razorpayXService;
    private final ModelMapper modelMapper;

    private static final int PAYOUT_CYCLE_DAYS = 7;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * Request a payout
     */
    @Transactional
    public PayoutDTO requestPayout(Long hostId, PayoutRequest request) {
        log.info("Payout request from host: {}, amount: {}", hostId, request.getAmount());
        
        if (request.getAmount().compareTo(economicsProperties.getMinPayoutAmount()) < 0) {
            throw new IllegalArgumentException("Minimum payout amount is " + economicsProperties.getMinPayoutAmount());
        }

        BigDecimal available = hostLedgerService.sumEarnedAvailable(hostId);
        if (request.getAmount().compareTo(available) > 0) {
            throw new IllegalArgumentException("Requested amount exceeds available balance");
        }

        BigDecimal transferAmount = request.getAmount();

        Payout payout = new Payout();
        payout.setHostId(hostId);
        payout.setBankAccountId(request.getBankAccountId());
        payout.setAmount(transferAmount);
        payout.setGrossAmount(transferAmount);
        payout.setCommissionAmount(BigDecimal.ZERO);
        payout.setTaxesAmount(BigDecimal.ZERO);
        payout.setPayoutFees(BigDecimal.ZERO);
        payout.setNetAmount(transferAmount);
        payout.setPayoutStatus("PENDING");
        payout.setPayoutPeriodStart(request.getPayoutPeriodStart());
        payout.setPayoutPeriodEnd(request.getPayoutPeriodEnd());
        payout.setDescription(request.getDescription());
        payout.setNotes(request.getNotes());
        payout.setBookingIds(request.getBookingIds());
        payout.setRetryCount(0);
        payout.setRequestedDate(LocalDateTime.now());
        payout.setExpectedCompletionDate(LocalDate.now().plusDays(PAYOUT_CYCLE_DAYS));
        
        payout = payoutRepository.save(payout);
        hostLedgerService.allocateEarnedFifo(hostId, payout.getId(), transferAmount);

        log.info("Payout requested successfully for host: {} with ID: {}", hostId, payout.getId());
        return modelMapper.map(payout, PayoutDTO.class);
    }
    
    /**
     * Get payout status
     */
    @Transactional(readOnly = true)
    public PayoutStatusDTO getPayoutStatus(Long payoutId) {
        log.info("Fetching payout status for ID: {}", payoutId);
        
        var payout = payoutRepository.findById(payoutId)
            .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        
        PayoutStatusDTO statusDTO = new PayoutStatusDTO();
        statusDTO.setPayoutId(payout.getId());
        statusDTO.setStatus(payout.getPayoutStatus());
        statusDTO.setAmount(payout.getAmount());
        statusDTO.setNetAmount(payout.getNetAmount());
        statusDTO.setRequestedDate(payout.getRequestedDate());
        statusDTO.setCompletedDate(payout.getCompletedDate());
        statusDTO.setExpectedCompletionDate(payout.getExpectedCompletionDate());
        statusDTO.setTransactionReference(payout.getTransactionReference());
        statusDTO.setFailureReason(payout.getFailureReason());
        statusDTO.setRetryCount(payout.getRetryCount());
        
        // Calculate progress percentage
        int progress = calculatePayoutProgress(payout.getPayoutStatus());
        statusDTO.setProgressPercentage(progress);
        
        return statusDTO;
    }
    
    /**
     * Get payout history for a host (paginated)
     */
    @Transactional(readOnly = true)
    public Page<PayoutHistoryDTO> getPayoutHistory(Long hostId, Pageable pageable) {
        log.info("Fetching payout history for host: {}", hostId);
        
        Page<Payout> payouts = payoutRepository.findByHostIdPaginated(hostId, pageable);
        
        return payouts.map(p -> modelMapper.map(p, PayoutHistoryDTO.class));
    }
    
    /**
     * Get payout history filtered by status
     */
    @Transactional(readOnly = true)
    public Page<PayoutHistoryDTO> getPayoutHistoryByStatus(Long hostId, String status, Pageable pageable) {
        log.info("Fetching {} payouts for host: {}", status, hostId);
        
        Page<Payout> payouts = payoutRepository.findByHostIdAndStatusPaginated(hostId, status, pageable);
        
        return payouts.map(p -> modelMapper.map(p, PayoutHistoryDTO.class));
    }
    
    /**
     * Approve a payout (admin operation)
     */
    @Transactional
    public PayoutDTO approvePayout(Long payoutId, String approvalNotes) {
        log.info("Approving payout: {}", payoutId);
        
        var payout = payoutRepository.findById(payoutId)
            .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        
        if (!"PENDING".equals(payout.getPayoutStatus())) {
            throw new IllegalStateException("Only PENDING payouts can be approved");
        }
        
        payout.setPayoutStatus("APPROVED");
        payout.setApprovedDate(LocalDateTime.now());
        payout.setNotes(approvalNotes);
        payout.setUpdatedAt(LocalDateTime.now());
        
        payout = payoutRepository.save(payout);
        
        log.info("Payout approved: {}", payoutId);
        return modelMapper.map(payout, PayoutDTO.class);
    }
    
    /**
     * Process a payout (mark as processing and send to payment gateway)
     */
    @Transactional
    public PayoutDTO processPayout(Long payoutId, String paymentMethod) {
        log.info("Processing payout: {} with method: {}", payoutId, paymentMethod);
        
        var payout = payoutRepository.findById(payoutId)
            .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        
        if (!"APPROVED".equals(payout.getPayoutStatus())) {
            throw new IllegalStateException("Only APPROVED payouts can be processed");
        }
        
        payout.setPayoutStatus("PROCESSING");
        payout.setProcessedDate(LocalDateTime.now());
        payout.setPaymentMethod(paymentMethod);
        payout.setUpdatedAt(LocalDateTime.now());
        
        payout = payoutRepository.save(payout);

        // Attempt the actual bank transfer via Razorpay Payouts API.
        // When Razorpay X is configured this completes immediately; until then
        // initiatePayoutTransfer() throws UnsupportedOperationException and the
        // payout remains in PROCESSING for manual settlement.
        try {
            String fundAccountId = resolveFundAccountId(payout.getBankAccountId());
            String txRef = razorpayIntegrationService.initiatePayoutTransfer(
                fundAccountId,
                payout.getNetAmount(),
                "INR",
                "Host payout #" + payoutId
            );
            payout.setTransactionReference(txRef);
            if (razorpayXService.completesPayoutSynchronously()) {
                payout.setPayoutStatus("COMPLETED");
                payout.setCompletedDate(LocalDateTime.now());
                payout.setActualCompletionDate(LocalDate.now());
                payout = payoutRepository.save(payout);
                hostLedgerService.markPaidForPayout(payoutId);
                log.info("Payout {} completed via gateway, tx ref: {}", payoutId, txRef);
            } else {
                payout = payoutRepository.save(payout);
                log.info("Payout {} submitted to Razorpay X, tx ref: {} (awaiting webhook)", payoutId, txRef);
            }
        } catch (Exception e) {
            payout.setPayoutStatus("FAILED");
            payout.setFailureReason("Gateway error: " + e.getMessage());
            payout = payoutRepository.save(payout);
            hostLedgerService.revertPayoutPending(payoutId);
            log.error("Payout {} gateway transfer failed: {}", payoutId, e.getMessage(), e);
        }

        log.info("Payout {} processed (status: {})", payoutId, payout.getPayoutStatus());
        return modelMapper.map(payout, PayoutDTO.class);
    }
    
    /**
     * Complete a payout (mark as completed, update status from payment gateway)
     */
    @Transactional
    public PayoutDTO completePayout(Long payoutId, String transactionReference) {
        log.info("Completing payout: {} with transaction: {}", payoutId, transactionReference);
        
        var payout = payoutRepository.findById(payoutId)
            .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        
        if (!"PROCESSING".equals(payout.getPayoutStatus())) {
            throw new IllegalStateException("Only PROCESSING payouts can be completed");
        }
        
        payout.setPayoutStatus("COMPLETED");
        payout.setCompletedDate(LocalDateTime.now());
        payout.setActualCompletionDate(LocalDate.now());
        payout.setTransactionReference(transactionReference);
        payout.setRetryCount(0); // Reset retry count on success
        payout.setUpdatedAt(LocalDateTime.now());
        
        payout = payoutRepository.save(payout);
        hostLedgerService.markPaidForPayout(payoutId);

        log.info("Payout completed: {} with transaction: {}", payoutId, transactionReference);
        return modelMapper.map(payout, PayoutDTO.class);
    }
    
    /**
     * Mark payout as failed and queue for retry
     */
    @Transactional
    public PayoutDTO failPayout(Long payoutId, String failureReason) {
        log.warn("Marking payout as failed: {} - {}", payoutId, failureReason);
        
        var payout = payoutRepository.findById(payoutId)
            .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        
        payout.setPayoutStatus("FAILED");
        payout.setFailureReason(failureReason);
        payout.setRetryCount(payout.getRetryCount() + 1);
        payout.setLastRetryDate(LocalDateTime.now());
        payout.setUpdatedAt(LocalDateTime.now());
        
        payout = payoutRepository.save(payout);
        hostLedgerService.revertPayoutPending(payoutId);

        log.info("Payout marked as failed: {} (retry attempt: {})", payoutId, payout.getRetryCount());
        return modelMapper.map(payout, PayoutDTO.class);
    }
    
    /**
     * Get available balance for a host
     */
    @Transactional(readOnly = true)
    public PayoutBalanceDTO getHostBalance(Long hostId) {
        log.info("Fetching balance for host: {}", hostId);
        
        BigDecimal totalPayouts = payoutRepository.sumCompletedPayoutsByHostId(hostId);
        if (totalPayouts == null) {
            totalPayouts = BigDecimal.ZERO;
        }

        BigDecimal pendingBalance = payoutRepository.sumPendingPayoutsByHostId(hostId);
        if (pendingBalance == null) {
            pendingBalance = BigDecimal.ZERO;
        }

        BigDecimal availableBalance = hostLedgerService.sumEarnedAvailable(hostId);
        BigDecimal pendingRelease = hostLedgerService.sumPendingRelease(hostId);
        BigDecimal totalEarnings = availableBalance.add(pendingRelease)
            .add(pendingBalance)
            .add(totalPayouts);

        PayoutBalanceDTO balanceDTO = new PayoutBalanceDTO();
        balanceDTO.setHostId(hostId);
        balanceDTO.setAvailableBalance(availableBalance);
        balanceDTO.setPendingBalance(pendingBalance);
        balanceDTO.setPendingReleaseBalance(pendingRelease);
        balanceDTO.setTotalEarnings(totalEarnings);
        balanceDTO.setTotalPayouts(totalPayouts);
        balanceDTO.setNetEarnings(availableBalance);
        balanceDTO.setCurrency(economicsProperties.getCurrency());
        balanceDTO.setMinPayoutAmount(economicsProperties.getMinPayoutAmount());
        
        // Get last completed payout date
        var lastPayout = payoutRepository.findLastCompletedPayoutByHostId(hostId);
        if (lastPayout.isPresent()) {
            balanceDTO.setLastPayoutDate(lastPayout.get().getCompletedDate());
            LocalDate nextPayoutDate = lastPayout.get().getCompletedDate().toLocalDate().plusDays(PAYOUT_CYCLE_DAYS);
            int daysRemaining = (int) java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), nextPayoutDate);
            balanceDTO.setNextPayoutDaysRemaining(Math.max(0, daysRemaining));
        } else {
            balanceDTO.setNextPayoutDaysRemaining(PAYOUT_CYCLE_DAYS);
        }
        
        return balanceDTO;
    }
    
    /**
     * Get list of payouts for admin review
     */
    @Transactional(readOnly = true)
    public List<PayoutDTO> getPendingPayoutsForApproval() {
        log.info("Fetching pending payouts for admin approval");
        
        return payoutRepository.findByPayoutStatus("PENDING")
            .stream()
            .map(p -> modelMapper.map(p, PayoutDTO.class))
            .collect(Collectors.toList());
    }
    
    /**
     * Retry failed payouts
     */
    @Transactional
    public void retryFailedPayouts() {
        log.info("Processing retry for failed payouts");
        
        var failedPayouts = payoutRepository.findFailedPayoutsForRetry();
        
        for (Payout payout : failedPayouts) {
            if (payout.getRetryCount() < MAX_RETRY_ATTEMPTS) {
                log.info("Retrying payout: {} (attempt: {})", payout.getId(), payout.getRetryCount() + 1);
                
                payout.setLastRetryDate(LocalDateTime.now());
                payout.setUpdatedAt(LocalDateTime.now());

                // Attempt the gateway transfer directly on retry
                try {
                    String fundAccountId = resolveFundAccountId(payout.getBankAccountId());
                    String txRef = razorpayIntegrationService.initiatePayoutTransfer(
                        fundAccountId,
                        payout.getNetAmount(),
                        "INR",
                        "Host payout retry #" + payout.getId()
                    );
                    payout.setTransactionReference(txRef);
                    payout.setRetryCount(0);
                    if (razorpayXService.completesPayoutSynchronously()) {
                        payout.setPayoutStatus("COMPLETED");
                        payout.setCompletedDate(LocalDateTime.now());
                        payout.setActualCompletionDate(LocalDate.now());
                        payoutRepository.save(payout);
                        hostLedgerService.markPaidForPayout(payout.getId());
                        log.info("Payout {} completed on retry, tx ref: {}", payout.getId(), txRef);
                    } else {
                        payout.setPayoutStatus("PROCESSING");
                        payoutRepository.save(payout);
                        log.info("Payout {} resubmitted on retry, tx ref: {}", payout.getId(), txRef);
                    }
                } catch (Exception e) {
                    // Keep as FAILED; retry counter was already incremented above
                    payoutRepository.save(payout);
                    log.error("Payout {} retry gateway call failed: {}", payout.getId(), e.getMessage(), e);
                }
            } else {
                log.warn("Payout {} exceeded max retry attempts", payout.getId());
            }
        }
    }
    
    /**
     * Cancel a pending payout
     */
    @Transactional
    public PayoutDTO cancelPayout(Long payoutId, String cancellationReason) {
        log.info("Cancelling payout: {}", payoutId);
        
        var payout = payoutRepository.findById(payoutId)
            .orElseThrow(() -> new IllegalArgumentException("Payout not found: " + payoutId));
        
        if (!("PENDING".equals(payout.getPayoutStatus()) || "APPROVED".equals(payout.getPayoutStatus()))) {
            throw new IllegalStateException("Only PENDING or APPROVED payouts can be cancelled");
        }
        
        payout.setPayoutStatus("CANCELLED");
        payout.setNotes(cancellationReason);
        payout.setUpdatedAt(LocalDateTime.now());
        
        payout = payoutRepository.save(payout);
        hostLedgerService.revertPayoutPending(payoutId);

        log.info("Payout cancelled: {}", payoutId);
        return modelMapper.map(payout, PayoutDTO.class);
    }

    @Transactional
    public void handleRazorpayPayoutWebhook(String payload, String signature) {
        if (signature == null || signature.isBlank()) {
            throw new IllegalArgumentException("Missing Razorpay webhook signature");
        }
        if (!razorpayXService.verifyWebhookSignature(payload, signature)) {
            throw new IllegalArgumentException("Invalid Razorpay webhook signature");
        }
        org.json.JSONObject event = new org.json.JSONObject(payload);
        String eventType = event.optString("event");
        org.json.JSONObject payloadObj = event.optJSONObject("payload");
        if (payloadObj == null) {
            return;
        }
        org.json.JSONObject payoutObj = payloadObj.optJSONObject("payout");
        if (payoutObj == null) {
            return;
        }
        org.json.JSONObject entity = payoutObj.optJSONObject("entity");
        if (entity == null) {
            return;
        }
        String razorpayPayoutId = entity.optString("id");
        payoutRepository.findByTransactionReference(razorpayPayoutId).ifPresent(p -> {
            if ("payout.processed".equals(eventType)) {
                p.setPayoutStatus("COMPLETED");
                p.setCompletedDate(LocalDateTime.now());
                p.setActualCompletionDate(LocalDate.now());
                payoutRepository.save(p);
                hostLedgerService.markPaidForPayout(p.getId());
            } else if ("payout.failed".equals(eventType) || "payout.reversed".equals(eventType)) {
                p.setPayoutStatus("FAILED");
                p.setFailureReason(entity.optString("status_description", "Payout failed"));
                payoutRepository.save(p);
                hostLedgerService.revertPayoutPending(p.getId());
            }
        });
    }

    private String resolveFundAccountId(Long bankAccountId) {
        if (bankAccountId == null) {
            throw new IllegalArgumentException("Bank account is required for payout");
        }
        HostBankAccount bank = hostBankAccountRepository.findById(bankAccountId)
            .orElseThrow(() -> new IllegalArgumentException("Bank account not found"));
        if (bank.getRazorpayFundAccountId() == null || bank.getRazorpayFundAccountId().isBlank()) {
            throw new IllegalStateException("Bank account is not linked to Razorpay X — verify the account first");
        }
        return bank.getRazorpayFundAccountId();
    }

    /**
     * Get payouts by date range for a host
     */
    @Transactional(readOnly = true)
    public List<PayoutDTO> getPayoutsByDateRange(Long hostId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching payouts for host: {} from {} to {}", hostId, startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        
        return payoutRepository.findByHostIdAndDateRange(hostId, startDateTime, endDateTime)
            .stream()
            .map(p -> modelMapper.map(p, PayoutDTO.class))
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate progress percentage for payout status
     */
    private int calculatePayoutProgress(String status) {
        return switch (status) {
            case "PENDING" -> 25;
            case "APPROVED" -> 50;
            case "PROCESSING" -> 75;
            case "COMPLETED" -> 100;
            case "FAILED", "CANCELLED" -> 0;
            default -> 0;
        };
    }
}
