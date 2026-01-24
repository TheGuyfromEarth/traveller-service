package com.travolish.traveller.payout.service;

import com.travolish.traveller.payout.dto.*;
import com.travolish.traveller.payout.entity.Payout;
import com.travolish.traveller.payout.repository.PayoutRepository;
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
    private final ModelMapper modelMapper;
    
    // Configuration constants
    private static final BigDecimal COMMISSION_RATE = BigDecimal.valueOf(0.15); // 15% commission
    private static final BigDecimal TAX_RATE = BigDecimal.valueOf(0.10); // 10% tax
    private static final BigDecimal PAYOUT_FEE = BigDecimal.valueOf(2.50); // $2.50 per payout
    private static final int PAYOUT_CYCLE_DAYS = 7; // Weekly payouts
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    /**
     * Request a payout
     */
    @Transactional
    public PayoutDTO requestPayout(Long hostId, PayoutRequest request) {
        log.info("Payout request from host: {}, amount: {}", hostId, request.getAmount());
        
        // Validate amount
        if (request.getAmount().compareTo(BigDecimal.valueOf(10)) < 0) {
            throw new IllegalArgumentException("Minimum payout amount is $10");
        }
        
        // Calculate deductions
        BigDecimal grossAmount = request.getAmount();
        BigDecimal commissionAmount = grossAmount.multiply(COMMISSION_RATE);
        BigDecimal taxesAmount = grossAmount.multiply(TAX_RATE);
        BigDecimal netAmount = grossAmount.subtract(commissionAmount).subtract(taxesAmount).subtract(PAYOUT_FEE);
        
        // Ensure net amount is positive
        if (netAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Net payout amount would be negative");
        }
        
        Payout payout = new Payout();
        payout.setHostId(hostId);
        payout.setBankAccountId(request.getBankAccountId());
        payout.setAmount(request.getAmount());
        payout.setGrossAmount(grossAmount);
        payout.setCommissionAmount(commissionAmount);
        payout.setTaxesAmount(taxesAmount);
        payout.setPayoutFees(PAYOUT_FEE);
        payout.setNetAmount(netAmount);
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
        
        // TODO: Integrate with payment gateway (Stripe, Razorpay, etc.)
        // This would make the actual payout transfer
        
        log.info("Payout marked as processing: {}", payoutId);
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
        
        log.info("Payout marked as failed: {} (retry attempt: {})", payoutId, payout.getRetryCount());
        return modelMapper.map(payout, PayoutDTO.class);
    }
    
    /**
     * Get available balance for a host
     */
    @Transactional(readOnly = true)
    public PayoutBalanceDTO getHostBalance(Long hostId) {
        log.info("Fetching balance for host: {}", hostId);
        
        // Calculate totals from completed payouts
        BigDecimal totalPayouts = payoutRepository.sumCompletedPayoutsByHostId(hostId);
        if (totalPayouts == null) {
            totalPayouts = BigDecimal.ZERO;
        }
        
        // Calculate pending amounts
        BigDecimal pendingBalance = payoutRepository.sumPendingPayoutsByHostId(hostId);
        if (pendingBalance == null) {
            pendingBalance = BigDecimal.ZERO;
        }
        
        // TODO: Get total earnings from booking service
        BigDecimal totalEarnings = BigDecimal.valueOf(0); // Placeholder
        
        // Calculate derived values
        BigDecimal availableBalance = totalEarnings.subtract(totalPayouts).subtract(pendingBalance);
        if (availableBalance.compareTo(BigDecimal.ZERO) < 0) {
            availableBalance = BigDecimal.ZERO;
        }
        
        PayoutBalanceDTO balanceDTO = new PayoutBalanceDTO();
        balanceDTO.setHostId(hostId);
        balanceDTO.setAvailableBalance(availableBalance);
        balanceDTO.setPendingBalance(pendingBalance);
        balanceDTO.setTotalEarnings(totalEarnings);
        balanceDTO.setTotalPayouts(totalPayouts);
        
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
                
                // Reset to APPROVED for retry
                payout.setPayoutStatus("APPROVED");
                payout.setLastRetryDate(LocalDateTime.now());
                payout.setUpdatedAt(LocalDateTime.now());
                payoutRepository.save(payout);
                
                // TODO: Call payment gateway again
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
        
        log.info("Payout cancelled: {}", payoutId);
        return modelMapper.map(payout, PayoutDTO.class);
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
