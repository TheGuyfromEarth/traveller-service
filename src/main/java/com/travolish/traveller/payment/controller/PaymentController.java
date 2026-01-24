package com.travolish.traveller.payment.controller;

import com.travolish.traveller.payment.dto.*;
import com.travolish.traveller.payment.entity.PaymentMethod;
import com.travolish.traveller.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * Process payment transaction
     * POST /api/payments/process
     */
    @PostMapping("/process")
    public ResponseEntity<PaymentDTO> processPayment(
        @Valid @RequestBody PaymentRequest request,
        Authentication authentication) {
        try {
            log.info("Processing payment request from user: {}", authentication.getName());
            
            Long userId = extractUserIdFromAuth(authentication);
            PaymentDTO paymentDTO = paymentService.processPayment(userId, request);
            
            log.info("Payment processed successfully: {}", paymentDTO.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(paymentDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error processing payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Validate payment
     * POST /api/payments/validate
     */
    @PostMapping("/validate")
    public ResponseEntity<PaymentDTO> validatePayment(
        @Valid @RequestBody PaymentValidationRequest request,
        Authentication authentication) {
        try {
            log.info("Validating payment: {}", request.getRazorpayOrderId());
            
            PaymentDTO paymentDTO = paymentService.validatePayment(request);
            
            log.info("Payment validated successfully: {}", paymentDTO.getId());
            return ResponseEntity.ok(paymentDTO);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Payment validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error validating payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get user payment methods
     * GET /api/payments/methods
     */
    @GetMapping("/methods")
    public ResponseEntity<List<PaymentMethodDTO>> getPaymentMethods(
        Authentication authentication) {
        try {
            log.info("Fetching payment methods for user: {}", authentication.getName());
            
            Long userId = extractUserIdFromAuth(authentication);
            List<PaymentMethodDTO> methods = paymentService.getUserPaymentMethods(userId);
            
            return ResponseEntity.ok(methods);
        } catch (Exception e) {
            log.error("Error fetching payment methods", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Add payment method
     * POST /api/payments/methods
     */
    @PostMapping("/methods")
    public ResponseEntity<PaymentMethodDTO> addPaymentMethod(
        @Valid @RequestBody PaymentMethod paymentMethod,
        Authentication authentication) {
        try {
            log.info("Adding payment method for user: {}", authentication.getName());
            
            Long userId = extractUserIdFromAuth(authentication);
            PaymentMethodDTO methodDTO = paymentService.addPaymentMethod(userId, paymentMethod);
            
            log.info("Payment method added: {}", methodDTO.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(methodDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment method: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error adding payment method", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Remove payment method
     * DELETE /api/payments/methods/{id}
     */
    @DeleteMapping("/methods/{id}")
    public ResponseEntity<Void> removePaymentMethod(
        @PathVariable Long id,
        Authentication authentication) {
        try {
            log.info("Removing payment method: {} for user: {}", id, authentication.getName());
            
            Long userId = extractUserIdFromAuth(authentication);
            paymentService.removePaymentMethod(userId, id);
            
            log.info("Payment method removed: {}", id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Payment method not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (IllegalStateException e) {
            log.warn("Cannot remove payment method: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error removing payment method", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get receipt for booking
     * GET /api/payments/receipt/{bookingId}
     */
    @GetMapping("/receipt/{bookingId}")
    public ResponseEntity<ReceiptDTO> getReceipt(
        @PathVariable Long bookingId,
        Authentication authentication) {
        try {
            log.info("Fetching receipt for booking: {}", bookingId);
            
            ReceiptDTO receiptDTO = paymentService.getReceiptForBooking(bookingId);
            
            return ResponseEntity.ok(receiptDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Receipt not found for booking: {}", bookingId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching receipt", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Process refund
     * POST /api/payments/refund
     */
    @PostMapping("/refund")
    public ResponseEntity<RefundDTO> processRefund(
        @Valid @RequestBody RefundRequest request,
        Authentication authentication) {
        try {
            log.info("Processing refund for payment: {}", request.getPaymentId());
            
            Long userId = extractUserIdFromAuth(authentication);
            RefundDTO refundDTO = paymentService.processRefund(userId, request);
            
            log.info("Refund processed: {}", refundDTO.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(refundDTO);
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Refund request error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error processing refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get transaction details
     * GET /api/payments/transaction/{id}
     */
    @GetMapping("/transaction/{id}")
    public ResponseEntity<TransactionDTO> getTransaction(
        @PathVariable Long id,
        Authentication authentication) {
        try {
            log.info("Fetching transaction details: {}", id);
            
            TransactionDTO transactionDTO = paymentService.getTransactionDetails(id);
            
            return ResponseEntity.ok(transactionDTO);
        } catch (IllegalArgumentException e) {
            log.warn("Transaction not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get payment history
     * GET /api/payments/history
     */
    @GetMapping("/history")
    public ResponseEntity<Page<PaymentDTO>> getPaymentHistory(
        Pageable pageable,
        Authentication authentication) {
        try {
            log.info("Fetching payment history for user: {}", authentication.getName());
            
            Long userId = extractUserIdFromAuth(authentication);
            Page<PaymentDTO> history = paymentService.getPaymentHistory(userId, pageable);
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("Error fetching payment history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Helper method to extract user ID from authentication
     */
    private Long extractUserIdFromAuth(Authentication authentication) {
        // Placeholder - in production, extract from JWT token or session
        return 1L;
    }
}
