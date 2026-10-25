package com.travolish.traveller.payment.service;

import com.travolish.traveller.payment.dto.*;
import com.travolish.traveller.payment.entity.*;
import com.travolish.traveller.payment.repository.*;
import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.model.BookingPaymentConfig;
import com.travolish.traveller.booking.repository.BookingPaymentConfigRepository;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.booking.service.BookingAccessService;
import com.travolish.traveller.config.PlatformEconomicsProperties;
import com.travolish.traveller.payout.service.HostLedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.beans.factory.annotation.Value;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final BookingPaymentConfigRepository bookingPaymentConfigRepository;
    private final BookingAccessService bookingAccessService;
    private final PlatformEconomicsProperties economicsProperties;
    private final HostLedgerService hostLedgerService;
    private final ModelMapper modelMapper;
    
    @Value("${razorpay.api.key:rzp_test_placeholder}")
    private String razorpayKeyId;

    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUND = RoundingMode.HALF_UP;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int PENDING_EXPIRY_MINUTES = 15;
    
    /**
     * Process payment transaction
     */
    public PaymentDTO processPayment(Long userId, boolean isAdmin, PaymentRequest request) {
        log.info("Processing payment for user: {}, booking: {}", userId, request.getBookingId());
        
        validatePaymentRequest(userId, isAdmin, request);
        
        // Get payment method
        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
            .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));
        
        if (!paymentMethod.getUserId().equals(userId)) {
            throw new IllegalStateException("Payment method does not belong to user");
        }
        
        Booking booking = bookingRepository.findById(request.getBookingId())
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + request.getBookingId()));
        PaymentCheckoutAmounts amounts = computeCheckoutAmountsForBooking(booking);
        BigDecimal baseAmount = amounts.getBaseAmount();
        BigDecimal platformFee = amounts.getPlatformFee();
        BigDecimal taxAmount = amounts.getTaxAmount();
        BigDecimal totalAmount = amounts.getTotalAmount();
        BigDecimal netAmount = amounts.getNetAmount();

        cancelStalePendingPaymentsForBooking(request.getBookingId());

        // Create payment record first so Razorpay receipt references our payment id
        Payment payment = Payment.builder()
            .userId(userId)
            .bookingId(request.getBookingId())
            .paymentMethodId(request.getPaymentMethodId())
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .description(request.getDescription())
            .paymentStatus(PaymentStatus.PENDING)
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

        String orderId = razorpayService.createOrder(
            payment.getId(), totalAmount, request.getCurrency(),
            "Payment for booking: " + request.getBookingId());
        payment.setRazorpayOrderId(orderId);
        payment = paymentRepository.save(payment);
        log.info("Payment created with ID: {} and Razorpay order: {}", payment.getId(), orderId);

        return modelMapper.map(payment, PaymentDTO.class);
    }
    
    /**
     * Validate payment details
     */
    public PaymentDTO validatePayment(Long userId, boolean isAdmin, PaymentValidationRequest request) {
        log.info("Validating payment with order ID: {}", request.getRazorpayOrderId());

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        assertPaymentOwnerOrAdmin(userId, isAdmin, payment);

        if (PaymentStatus.COMPLETED.equals(payment.getPaymentStatus())) {
            return modelMapper.map(payment, PaymentDTO.class);
        }

        if (!razorpayService.verifyPaymentSignature(request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature())) {
            log.warn("Payment signature verification failed for order: {}", request.getRazorpayOrderId());
            throw new IllegalStateException("Payment signature verification failed");
        }
        
        // Update payment with Razorpay details
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setPaymentStatus(PaymentStatus.PROCESSING);
        payment.setProcessedAt(LocalDateTime.now());
        payment.setMetadata(request.getMetadata());
        
        // Fetch payment details from Razorpay for additional verification
        try {
            org.json.JSONObject paymentDetails = razorpayService.getPaymentDetails(request.getRazorpayPaymentId());
            
            String razorpayStatus = paymentDetails.getString("status");
            if ("captured".equalsIgnoreCase(razorpayStatus)) {
                applyCapturedPayment(payment, request.getRazorpayPaymentId(), paymentDetails);
                log.info("Payment completed successfully: {}", payment.getId());
            } else if ("failed".equalsIgnoreCase(razorpayStatus)) {
                payment.setPaymentStatus(PaymentStatus.FAILED);
                payment.setFailedAt(LocalDateTime.now());
                payment.setFailureReason(paymentDetails.optString("description"));
                log.warn("Payment failed for order: {}", request.getRazorpayOrderId());
            }
            
        } catch (Exception e) {
            log.error("Error fetching payment details from Razorpay: {}", e.getMessage(), e);
            throw new IllegalStateException("Unable to verify payment with Razorpay");
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
        
        BigDecimal refundable = payment.getTotalAmount() != null ? payment.getTotalAmount() : payment.getAmount();
        if (request.getRefundAmount().compareTo(refundable) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed payment total");
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
        if (request.getRefundAmount().compareTo(refundable) == 0) {
            payment.setPaymentStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        }
        
        log.info("Refund processed with ID: {} for payment: {}", refund.getId(), payment.getId());
        return modelMapper.map(refund, RefundDTO.class);
    }
    
    /**
     * Get transaction details
     */
    public TransactionDTO getTransactionDetails(Long userId, boolean isAdmin, Long transactionId) {
        log.info("Fetching transaction details: {}", transactionId);
        
        Payment payment = paymentRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        assertPaymentOwnerOrAdmin(userId, isAdmin, payment);
        if (payment.getBookingId() != null) {
            bookingAccessService.assertGuestHostOrAdmin(userId, isAdmin, payment.getBookingId());
        }
        
        return modelMapper.map(payment, TransactionDTO.class);
    }
    
    /**
     * Get receipt for booking
     */
    public ReceiptDTO getReceiptForBooking(Long userId, boolean isAdmin, Long bookingId) {
        log.info("Fetching receipt for booking: {}", bookingId);

        bookingAccessService.assertGuestHostOrAdmin(userId, isAdmin, bookingId);
        
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
    
    private void validatePaymentRequest(Long userId, boolean isAdmin, PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + request.getBookingId()));
        bookingAccessService.assertBookingGuestOrAdmin(userId, isAdmin, booking);

        PaymentCheckoutAmounts amounts = computeCheckoutAmountsForBooking(booking);
        if (amounts.getTotalAmount().compareTo(BigDecimal.ONE) < 0) {
            throw new IllegalArgumentException("Minimum payment amount is 1.00");
        }
        if (amounts.getTotalAmount().compareTo(new BigDecimal(10000000)) > 0) {
            throw new IllegalArgumentException("Maximum payment amount is 10,000,000");
        }
    }
    
    private BigDecimal moneyPercent(BigDecimal amount, String percent) {
        return amount.multiply(new BigDecimal(percent))
            .divide(new BigDecimal(100), MONEY_SCALE, MONEY_ROUND);
    }

    private BigDecimal calculatePlatformFee(BigDecimal amount) {
        return moneyPercent(amount, economicsProperties.getPlatformFeePercent().toPlainString());
    }

    private BigDecimal calculateTax(BigDecimal amount) {
        return moneyPercent(amount, economicsProperties.getGstPercent().toPlainString());
    }

    static long toPaise(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, MONEY_ROUND).movePointRight(2).longValueExact();
    }
    
    private void createReceiptIfAbsent(Payment payment) {
        if (receiptRepository.findByPaymentId(payment.getId()).isPresent()) {
            return;
        }
        createReceipt(payment);
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
        hostLedgerService.markRefundedForBooking(bookingId);

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
     * Step 1 of checkout: persist a PENDING payment, create Razorpay order, return both ids.
     */
    public PaymentCheckoutOrderResult createRazorpayOrder(Long userId, boolean isAdmin, String currency, Long bookingId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (bookingId == null) {
            throw new IllegalArgumentException("bookingId is required");
        }
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        bookingAccessService.assertBookingGuestOrAdmin(userId, isAdmin, booking);

        PaymentCheckoutAmounts amounts = computeCheckoutAmountsForBooking(booking);
        cancelStalePendingPaymentsForBooking(bookingId);

        String resolvedCurrency = (currency != null && !currency.isBlank()) ? currency : "INR";

        Payment payment = Payment.builder()
            .userId(userId)
            .bookingId(bookingId)
            .amount(amounts.getBaseAmount())
            .currency(resolvedCurrency)
            .description("Travolish booking #" + bookingId)
            .paymentStatus(PaymentStatus.PENDING)
            .paymentMethod("RAZORPAY_CHECKOUT")
            .baseAmount(amounts.getBaseAmount())
            .platformFee(amounts.getPlatformFee())
            .taxAmount(amounts.getTaxAmount())
            .totalAmount(amounts.getTotalAmount())
            .netAmount(amounts.getNetAmount())
            .initiatedAt(LocalDateTime.now())
            .retryCount(0)
            .isSecure(true)
            .paymentSource("WEB")
            .build();

        payment = paymentRepository.save(payment);

        String orderId = razorpayService.createOrder(
            payment.getId(),
            amounts.getTotalAmount(),
            resolvedCurrency,
            "Travolish booking payment #" + bookingId);
        payment.setRazorpayOrderId(orderId);
        payment = paymentRepository.save(payment);

        log.info("Checkout order {} for payment {} booking {}", orderId, payment.getId(), bookingId);
        return new PaymentCheckoutOrderResult(orderId, payment.getId(), amounts.getTotalAmount(), resolvedCurrency);
    }

    public record PaymentCheckoutOrderResult(String orderId, Long paymentId, BigDecimal amount, String currency) {}

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
                paymentRepository.findByRazorpayOrderId(razorpayOrderId).ifPresent(p -> {
                    if (!PaymentStatus.COMPLETED.equals(p.getPaymentStatus())) {
                        try {
                            JSONObject details = razorpayService.getPaymentDetails(razorpayPaymentId);
                            applyCapturedPayment(p, razorpayPaymentId, details);
                            paymentRepository.save(p);
                        } catch (Exception ex) {
                            log.error("Webhook capture handling failed for payment {}: {}", p.getId(), ex.getMessage());
                        }
                    }
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
                String refundId = entity.optString("id");
                refundRepository.findByRazorpayRefundId(refundId).ifPresent(r -> {
                    r.setRefundStatus(RefundStatus.COMPLETED);
                    r.setCompletedAt(LocalDateTime.now());
                    refundRepository.save(r);
                });
                log.info("Refund processed for order {}", razorpayOrderId);
            }
        } catch (Exception e) {
            log.error("Error handling Razorpay webhook: {}", e.getMessage(), e);
        }
    }

    PaymentCheckoutAmounts computeCheckoutAmountsForBooking(Booking booking) {
        BookingPaymentConfig config = bookingPaymentConfigRepository.findByHotelId(booking.getHotelId())
            .orElseGet(() -> BookingPaymentConfig.builder()
                .hotelId(booking.getHotelId())
                .payFullAtBooking(true)
                .build());

        boolean payAtPropertyOnly = Boolean.TRUE.equals(config.getPayAtProperty())
            && !Boolean.TRUE.equals(config.getPayFullAtBooking())
            && !Boolean.TRUE.equals(config.getSecureWithPartialPayment());
        if (payAtPropertyOnly) {
            throw new IllegalStateException("This property accepts payment at check-in only");
        }

        BigDecimal bookingTotal = BigDecimal.valueOf(booking.getTotalPrice());
        BigDecimal base;
        if (Boolean.TRUE.equals(config.getSecureWithPartialPayment())
                && config.getAdvancePaymentPercent() != null
                && config.getAdvancePaymentPercent() > 0) {
            base = moneyPercent(bookingTotal, config.getAdvancePaymentPercent().toString());
        } else {
            base = bookingTotal.setScale(MONEY_SCALE, MONEY_ROUND);
        }

        BigDecimal platformFee = calculatePlatformFee(base);
        BigDecimal taxAmount = calculateTax(base.add(platformFee));
        BigDecimal totalAmount = base.add(platformFee).add(taxAmount).setScale(MONEY_SCALE, MONEY_ROUND);

        return PaymentCheckoutAmounts.builder()
            .baseAmount(base)
            .platformFee(platformFee)
            .taxAmount(taxAmount)
            .totalAmount(totalAmount)
            .netAmount(base)
            .build();
    }

    private void cancelStalePendingPaymentsForBooking(Long bookingId) {
        for (Payment pending : paymentRepository.findByBookingIdAndPaymentStatus(bookingId, PaymentStatus.PENDING)) {
            pending.setPaymentStatus(PaymentStatus.CANCELLED);
            pending.setFailureReason("Superseded by new checkout");
            paymentRepository.save(pending);
        }
    }

    private void assertPaymentOwnerOrAdmin(Long userId, boolean isAdmin, Payment payment) {
        if (isAdmin) {
            return;
        }
        if (userId == null || payment.getUserId() == null || !payment.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this payment");
        }
    }

    private void verifyRazorpayAmount(Payment payment, JSONObject paymentDetails) {
        if (payment.getTotalAmount() == null) {
            return;
        }
        long expectedPaise = toPaise(payment.getTotalAmount());
        long actualPaise = paymentDetails.getLong("amount");
        if (expectedPaise != actualPaise) {
            throw new IllegalStateException("Payment amount does not match order");
        }
    }

    private void applyCapturedPayment(Payment payment, String razorpayPaymentId, JSONObject paymentDetails) {
        if (PaymentStatus.COMPLETED.equals(payment.getPaymentStatus())) {
            return;
        }
        verifyRazorpayAmount(payment, paymentDetails);
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        payment.setTransactionReference(paymentDetails.optString("id"));
        createReceiptIfAbsent(payment);
        maybeConfirmBookingAfterPayment(payment);
        hostLedgerService.recordPaymentCaptured(payment);
    }

    private void maybeConfirmBookingAfterPayment(Payment payment) {
        if (payment.getBookingId() == null) {
            return;
        }
        bookingRepository.findById(payment.getBookingId()).ifPresent(booking -> {
            if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
                return;
            }
            BookingPaymentConfig config = bookingPaymentConfigRepository.findByHotelId(booking.getHotelId())
                .orElse(null);
            boolean autoConfirm = config == null
                || Boolean.TRUE.equals(config.getPayFullAtBooking())
                || Boolean.TRUE.equals(config.getSecureWithPartialPayment());
            if (!autoConfirm) {
                return;
            }
            if (booking.getStatus() == Booking.BookingStatus.PENDING) {
                booking.setStatus(Booking.BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                log.info("Booking {} auto-confirmed after payment {}", booking.getId(), payment.getId());
            }
        });
    }
}
