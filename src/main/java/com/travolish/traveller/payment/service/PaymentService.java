package com.travolish.traveller.payment.service;

import com.travolish.traveller.payment.dto.*;
import com.travolish.traveller.payment.entity.*;
import com.travolish.traveller.payment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
    private final ModelMapper modelMapper;
    
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
}
