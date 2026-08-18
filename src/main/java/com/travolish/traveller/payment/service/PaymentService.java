package com.travolish.traveller.payment.service;

import com.travolish.traveller.payment.dto.*;
import com.travolish.traveller.payment.entity.*;
import com.travolish.traveller.payment.repository.*;
import com.travolish.traveller.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final RefundRepository refundRepository;
    private final ReceiptRepository receiptRepository;
    private final RazorpayIntegrationService razorpayService;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;
    
    @Value("${razorpay.api.key:rzp_test_placeholder}")
    private String razorpayKeyId;

    // Configuration Constants
    private static final String PLATFORM_FEE_PERCENTAGE = "2.5";
    private static final String TAX_PERCENTAGE = "18";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int PENDING_EXPIRY_MINUTES = 15;
    
    /**
     * Process payment transaction
     */
    public PaymentDTO processPayment(Long userId, PaymentRequest request) {
        log.info("Processing payment for user: {}, booking: {}, amount: {}", userId, request.getBookingId(), request.getAmount());
        
        // Validate booking exists (would integrate with booking service)
        validatePaymentRequest(userId, request);
        
        // Get payment method
        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
            .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
        
        if (!paymentMethod.getUserId().equals(userId)) {
            throw new IllegalStateException("Payment method does not belong to user");
        }
        
        // Calculate amounts
        BigDecimal baseAmount = request.getAmount();
        BigDecimal platformFee = calculatePlatformFee(baseAmount);
        BigDecimal taxAmount = calculateTax(baseAmount.add(platformFee));
        BigDecimal totalAmount = baseAmount.add(platformFee).add(taxAmount);
        BigDecimal netAmount = baseAmount;
        
        // Create Razorpay order
        String orderId = razorpayService.createOrder(null, totalAmount, request.getCurrency(), "Payment for booking: " + request.getBookingId());
        
        // Create payment record
        Payment payment = Payment.builder()
            .userId(userId)
            .bookingId(request.getBookingId())
            .paymentMethodId(request.getPaymentMethodId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .description(request.getDescription())
            .paymentStatus(PaymentStatus.PENDING)
            .razorpayOrderId(orderId)
            .paymentMethod(paymentMethod.getMethodType().toString())
            .baseAmount(baseAmount)
            .platformFee(platformFee)
            .taxAmount(taxAmount)
            .totalAmount(totalAmount)
            .netAmount(netAmount)
            .initiatedAt(LocalDateTime.now())
            .ipAddress(request.getIpAddress())
            .userAgent(request.getUserAgent())
            .metadata(request.getMetadata())
            .retryCount(0)
            .isSecure(true)
            .paymentSource("WEB")
            .build();
        
        // Save card details if present
        if (PaymentMethodType.CARD.equals(paymentMethod.getMethodType())) {
            payment.setCardLast4(paymentMethod.getCardLast4());
        }
        
        payment = paymentRepository.save(payment);
        log.info("Payment created with ID: {} and Razorpay order: {}", payment.getId(), orderId);
        
        return modelMapper.map(payment, PaymentDTO.class);
    }
    
    /**
     * Validate payment details
     */
    public PaymentDTO validatePayment(PaymentValidationRequest request) {
        log.info("Validating payment with order ID: {}", request.getRazorpayOrderId());
        
        // Verify signature
        if (!razorpayService.verifyPaymentSignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature())) {
            log.warn("Payment signature verification failed for order: {}", request.getRazorpayOrderId());
            throw new IllegalStateException("Payment signature verification failed");
        }
        
        // Find payment by Razorpay order
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        // Update payment with Razorpay details
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setPaymentStatus(PaymentStatus.PROCESSING);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setMetadata(request.getMetadata());
        
        // Fetch payment details from Razorpay for additional verification
        try {
            org.json.JSONObject paymentDetails = razorpayService.getPaymentDetails(request.getRazorpayPaymentId());
            
            // Update status based on Razorpay response
            String razorpayStatus = paymentDetails.getString("status");
            if ("captured".equalsIgnoreCase(razorpayStatus)) {
                payment.setPaymentStatus(PaymentStatus.COMPLETED);
                payment.setCompletedAt(LocalDateTime.now());
                payment.setTransactionReference(paymentDetails.optString("id"));
                
                // Create receipt
                createReceipt(payment);
                
                log.info("Payment completed successfully: {}", payment.getId());
            } else if ("failed".equalsIgnoreCase(razorpayStatus)) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setFailedAt(LocalDateTime.now());
                payment.setFailureReason(paymentDetails.optString("description"));
                log.warn("Payment failed for order: {}", request.getRazorpayOrderId());
            }
            
        } catch (Exception e) {
            log.error("Error fetching payment details from Razorpay: {}", e.getMessage(), e);
        }
        
        payment = paymentRepository.save(payment);
        return modelMapper.map(payment, PaymentDTO.class);
    }
    
    /**
     * Add payment method
     */
    public PaymentMethodDTO addPaymentMethod(Long userId, PaymentMethod paymentMethod) {
        log.info("Adding payment method for user: {}, type: {}", userId, paymentMethod.getMethodType());
        
        paymentMethod.setUserId(userId);
        paymentMethod.setIsActive(true);
        paymentMethod.setIsVerified(false);
        paymentMethod.setUsageCount(0);
        
        // Check for duplicate card
        if (PaymentMethodType.CARD.equals(paymentMethod.getMethodType())) {
            boolean exists = paymentMethodRepository.existsByUserIdAndCardLast4AndCardExpiryMonthAndCardExpiryYear(
                userId, paymentMethod.getCardLast4(), paymentMethod.getCardExpiryMonth(), paymentMethod.getCardExpiryYear()
            );
            if (exists) {
                throw new IllegalArgumentException("This card already exists");
            }
        }
        
        // Check for duplicate UPI
        if (PaymentMethodType.UPI.equals(paymentMethod.getMethodType())) {
            if (paymentMethodRepository.findByUpiVpa(paymentMethod.getUpiVpa()).isPresent()) {
                throw new IllegalArgumentException("This UPI address already exists");
            }
        }
        
        paymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Payment method added with ID: {}", paymentMethod.getId());
        
        return modelMapper.map(paymentMethod, PaymentMethodDTO.class);
    }
    
    /**
     * Get user payment methods
     */
    public List<PaymentMethodDTO> getUserPaymentMethods(Long userId) {
        log.info("Fetching payment methods for user: {}", userId);
        
        List<PaymentMethod> methods = paymentMethodRepository.findByUserId(userId);
        return methods.stream()
            .map(m -> modelMapper.map(m, PaymentMethodDTO.class))
            .collect(Collectors.toList());
    }
    
    /**
     * Remove payment method
     */
    public void removePaymentMethod(Long userId, Long methodId) {
        log.info("Removing payment method: {} for user: {}", methodId, userId);
        
        PaymentMethod method = paymentMethodRepository.findById(methodId)
            .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
        
        if (!method.getUserId().equals(userId)) {
            throw new IllegalStateException("Payment method does not belong to user");
        }
        
        method.setIsDeleted(true);
        paymentMethodRepository.save(method);
        log.info("Payment method removed: {}", methodId);
    }
    
    /**
     * Process refund
     */
    public RefundDTO processRefund(Long userId, RefundRequest request) {
        log.info("Processing refund for user: {}, payment: {}, amount: {}", userId, request.getPaymentId(), request.getRefundAmount());
        
        // Find payment
        Payment payment = paymentRepository.findById(request.getPaymentId())
            .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        
        if (!payment.getUserId().equals(userId)) {
            throw new IllegalStateException("Payment does not belong to user");
        }
        
        if (!PaymentStatus.COMPLETED.equals(payment.getPaymentStatus())) {
            throw new IllegalStateException("Only completed payments can be refunded");
        }
        
        // Check refund amount
        if (request.getRefundAmount().compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment amount");
        }
        
        // Process refund through Razorpay
        String razorpayRefundId = razorpayService.processRefund(payment.getRazorpayPaymentId(), request.getRefundAmount());
        
        // Create refund record
        Refund refund = Refund.builder()
            .paymentId(payment.getId())
            .userId(userId)
            .bookingId(payment.getBookingId())
            .refundAmount(request.getRefundAmount())
            .currency(payment.getCurrency())
            .reason(request.getReason())
            .refundStatus(RefundStatus.PROCESSING)
            .razorpayRefundId(razorpayRefundId)
            .razorpayPaymentId(payment.getRazorpayPaymentId())
            .notes(request.getNotes())
            .retryCount(0)
            .initiatedAt(LocalDateTime.now())
            .build();
        
        refund = refundRepository.save(refund);
        
        // Update payment status if full refund
        if (request.getRefundAmount().compareTo(payment.getAmount()) == 0) {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }
        
        log.info("Refund processed with ID: {} for payment: {}", refund.getId(), payment.getId());
        return modelMapper.map(refund, RefundDTO.class);
    }
    
    /**
     * Get transaction details
     */
    public TransactionDTO getTransactionDetails(Long transactionId) {
        log.info("Fetching transaction details: {}", transactionId);
        
        Payment payment = paymentRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        
        return modelMapper.map(payment, TransactionDTO.class);
    }
    
    /**
     * Get receipt for booking
     */
    public ReceiptDTO getReceiptForBooking(Long bookingId) {
        log.info("Fetching receipt for booking: {}", bookingId);
        
        Receipt receipt = receiptRepository.findByBookingId(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Receipt not found"));
        
        return modelMapper.map(receipt, ReceiptDTO.class);
    }
    
    /**
     * Get payment history for user
     */
    public Page<PaymentDTO> getPaymentHistory(Long userId, Pageable pageable) {
        log.info("Fetching payment history for user: {}", userId);
        
        return paymentRepository.findByUserId(userId, pageable)
            .map(p -> modelMapper.map(p, PaymentDTO.class));
    }
    
    /**
     * Retry failed payments
     */
    @Transactional
    public void retryFailedPayments() {
        log.info("Starting retry for failed payments");
        
        List<Payment> failedPayments = paymentRepository.findFailedPaymentsForRetry();
        
        for (Payment payment : failedPayments) {
            if (payment.getRetryCount() < MAX_RETRY_ATTEMPTS) {
                try {
                    log.info("Retrying payment: {} (attempt {})", payment.getId(), payment.getRetryCount() + 1);
                    
                    // Attempt to fetch latest status from Razorpay
                    razorpayService.getOrderDetails(payment.getRazorpayOrderId());
                    
                    payment.setRetryCount(payment.getRetryCount() + 1);
                    payment.setLastRetryAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                    
                } catch (Exception e) {
                    log.error("Error retrying payment: {}", payment.getId(), e);
                }
            }
        }
    }
    
    /**
     * Cancel expired pending payments
     */
    @Transactional
    public void cancelExpiredPayments() {
        log.info("Cancelling expired pending payments");
        
        LocalDateTime expiryTime = LocalDateTime.now().minusMinutes(PENDING_EXPIRY_MINUTES);
        List<Payment> expiredPayments = paymentRepository.findExpiredPendingPayments(expiryTime);
        
        for (Payment payment : expiredPayments) {
            payment.setPaymentStatus(PaymentStatus.CANCELLED);
            payment.setFailureReason("Payment expired");
            paymentRepository.save(payment);
            log.info("Payment cancelled due to expiry: {}", payment.getId());
        }
    }
    
    // Helper Methods
    
    private void validatePaymentRequest(Long userId, PaymentRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Minimum payment amount is 1.00");
        }

        if (request.getAmount().compareTo(new BigDecimal(10000000)) > 0) {
            throw new IllegalArgumentException("Maximum payment amount is 10,000,000");
        }

        // Verify the booking exists and belongs to the authenticated user
        if (request.getBookingId() != null) {
            bookingRepository.findById(request.getBookingId()).ifPresentOrElse(
                booking -> {
                    if (booking.getUserId() == null || !booking.getUserId().equals(userId)) {
                        throw new IllegalStateException(
                            "Booking #" + request.getBookingId() + " does not belong to the authenticated user");
                    }
                },
                () -> { throw new IllegalArgumentException("Booking not found: " + request.getBookingId()); }
            );
        }
    }
    
    private BigDecimal calculatePlatformFee(BigDecimal amount) {
        return amount.multiply(new BigDecimal(PLATFORM_FEE_PERCENTAGE)).divide(new BigDecimal(100));
    }
    
    private BigDecimal calculateTax(BigDecimal amount) {
        return amount.multiply(new BigDecimal(TAX_PERCENTAGE)).divide(new BigDecimal(100));
    }
    
    private void createReceipt(Payment payment) {
        try {
            Receipt receipt = Receipt.builder()
                .paymentId(payment.getId())
                .userId(payment.getUserId())
                .bookingId(payment.getBookingId())
                .receiptNumber("RCP-" + payment.getId() + "-" + System.currentTimeMillis())
                .receiptStatus(ReceiptStatus.GENERATED)
                .baseAmount(payment.getBaseAmount())
                .taxAmount(payment.getTaxAmount())
                .platformFee(payment.getPlatformFee())
                .totalAmount(payment.getTotalAmount())
                .generatedAt(LocalDateTime.now())
                .build();
            
            receiptRepository.save(receipt);
            log.info("Receipt created for payment: {}", payment.getId());
        } catch (Exception e) {
            log.error("Error creating receipt for payment: {}", payment.getId(), e);
        }
    }

    // ─── Booking-payment coordination helpers ────────────────────────────────

    /**
     * Returns {@code true} if a {@code COMPLETED} payment exists for the given booking.
     * Used by {@code BookingController.confirmBooking()} to warn when no payment is on
     * record before transitioning to {@code CONFIRMED}.
     */
    @Transactional(readOnly = true)
    public boolean hasCompletedPayment(Long bookingId) {
        return paymentRepository
            .findTopByBookingIdAndPaymentStatusOrderByCompletedAtDesc(bookingId, PaymentStatus.COMPLETED)
            .isPresent();
    }

    /**
     * Processes a refund when a booking is cancelled.
     *
     * <p>Looks up the most recent {@code COMPLETED} payment for the booking, calculates
     * the eligible refund amount according to the standard cancellation policy, and
     * initiates the refund through Razorpay. If no completed payment exists (e.g. a
     * pay-at-property booking) this is a no-op.
     *
     * <p>Cancellation policy:
     * <ul>
     *   <li>&gt; 7 days until check-in → full refund</li>
     *   <li>2–7 days until check-in → 50 % refund</li>
     *   <li>&lt; 2 days until check-in → non-refundable</li>
     * </ul>
     *
     * <p>Refund failures are logged but not re-thrown — the booking is already
     * cancelled at this point and manual follow-up can use the payment ID from logs.
     */
    @Transactional
    public void processRefundForBookingCancellation(Long bookingId, LocalDate checkInDate) {
        log.info("Evaluating refund for cancelled booking: {}", bookingId);

        Optional<Payment> paymentOpt = paymentRepository
            .findTopByBookingIdAndPaymentStatusOrderByCompletedAtDesc(bookingId, PaymentStatus.COMPLETED);

        if (paymentOpt.isEmpty()) {
            log.info("No completed payment found for booking {} — no refund to issue", bookingId);
            return;
        }

        Payment payment = paymentOpt.get();
        BigDecimal refundAmount = calculateCancellationRefundAmount(payment.getTotalAmount(), checkInDate);

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Cancellation policy yields zero refund for booking {} (check-in: {}) — skipping",
                    bookingId, checkInDate);
            return;
        }

        log.info("Issuing refund of {} for cancelled booking {} (payment id: {})",
                refundAmount, bookingId, payment.getId());

        RefundRequest refundReq = new RefundRequest();
        refundReq.setPaymentId(payment.getId());
        refundReq.setRefundAmount(refundAmount);
        refundReq.setReason("Booking #" + bookingId + " cancelled");

        try {
            processRefund(payment.getUserId(), refundReq);
        } catch (Exception e) {
            // Log for manual follow-up; do not re-throw — cancellation already persisted
            log.error("Refund failed for cancelled booking {} (payment {}): {}",
                    bookingId, payment.getId(), e.getMessage(), e);
        }
    }

    /**
     * Determines the refund amount for a cancellation based on how far out the
     * check-in date is. Mirrors the policy shown to guests on the TripDetailPage.
     */
    private BigDecimal calculateCancellationRefundAmount(BigDecimal totalAmount, LocalDate checkInDate) {
        if (checkInDate == null) {
            return totalAmount; // Unknown check-in — default to full refund
        }
        long daysUntilCheckIn = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), checkInDate);
        if (daysUntilCheckIn > 7) {
            return totalAmount;                                          // Full refund
        } else if (daysUntilCheckIn > 2) {
            return totalAmount.multiply(new BigDecimal("0.50"));        // 50 % refund
        } else {
            return BigDecimal.ZERO;                                      // Non-refundable
        }
    }

    // ─── Razorpay gateway helpers ────────────────────────────────────────────

    /** Returns the public Razorpay key ID for the frontend checkout widget. */
    public String getRazorpayKeyId() {
        return razorpayKeyId;
    }

    /**
     * Step 1 of checkout: create a Razorpay order and return its ID to the frontend.
     * The frontend opens the Razorpay checkout popup with this orderId, then calls
     * /api/payments/validate with the resulting paymentId + signature.
     */
    public String createRazorpayOrder(Long userId, BigDecimal amount, String currency, Long bookingId) {
        log.info("Creating Razorpay order for user {} booking {} amount {}", userId, bookingId, amount);
        return razorpayService.createOrder(bookingId, amount, currency,
            "Travolish booking payment" + (bookingId != null ? " #" + bookingId : ""));
    }

    /**
     * Razorpay webhook handler — processes payment.captured / payment.failed events.
     * Razorpay sends a POST to /api/payments/webhook/razorpay with an HMAC-SHA256 signature.
     */
    public void handleRazorpayWebhook(String payload, String signature) {
        log.info("Processing Razorpay webhook, payload size: {}", payload.length());

        // Reject immediately if the signature is missing or does not match.
        // An invalid signature means the request did not originate from Razorpay.
        if (signature == null || signature.isBlank()) {
            log.warn("Razorpay webhook received without X-Razorpay-Signature header — rejecting");
            throw new IllegalArgumentException("Missing Razorpay webhook signature");
        }
        if (!razorpayService.verifyWebhookSignature(payload, signature)) {
            log.warn("Razorpay webhook signature verification failed — rejecting");
            throw new IllegalArgumentException("Invalid Razorpay webhook signature");
        }

        try {
            JSONObject event = new JSONObject(payload);
            String eventType = event.optString("event");
            JSONObject payloadObj = event.optJSONObject("payload");
            if (payloadObj == null) return;

            JSONObject paymentEntity = payloadObj.optJSONObject("payment");
            if (paymentEntity == null) return;
            JSONObject entity = paymentEntity.optJSONObject("entity");
            if (entity == null) return;

            String razorpayOrderId = entity.optString("order_id");
            String razorpayPaymentId = entity.optString("id");

            if ("payment.captured".equals(eventType)) {
                // Mark payment as COMPLETED
                paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(p -> {
                    p.setPaymentStatus(PaymentStatus.COMPLETED);
                    p.setRazorpayPaymentId(razorpayPaymentId);
                    p.setCompletedAt(LocalDateTime.now());
                    paymentRepository.save(p);
                    createReceipt(p);
                    log.info("Payment {} marked COMPLETED via webhook", p.getId());
                });
            } else if ("payment.failed".equals(eventType)) {
                paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(p -> {
                    p.setPaymentStatus(PaymentStatus.FAILED);
                    p.setErrorMessage("Payment failed — " + entity.optString("error_description", "gateway error"));
                    paymentRepository.save(p);
                    log.warn("Payment {} marked FAILED via webhook", p.getId());
                });
            } else if ("refund.processed".equals(eventType)) {
                log.info("Refund processed for order {}", razorpayOrderId);
            }
        } catch (Exception e) {
            log.error("Error handling Razorpay webhook: {}", e.getMessage(), e);
        }
    }
}
