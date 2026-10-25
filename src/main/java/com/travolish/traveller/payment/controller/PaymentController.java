package com.travolish.traveller.payment.controller;

import com.travolish.traveller.payment.dto.*;
import com.travolish.traveller.payment.entity.PaymentMethod;
import com.travolish.traveller.payment.service.PaymentService;
import com.travolish.traveller.user.security.AuthSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PaymentController {
    
    private final PaymentService paymentService;
    
    @PostMapping("/process")
    public ResponseEntity<PaymentDTO> processPayment(
        @Valid @RequestBody PaymentRequest request,
        Authentication authentication) {
        try {
            Long userId = requireUserId(authentication);
            log.info("Processing payment request from user: {}", userId);
            PaymentDTO paymentDTO = paymentService.processPayment(
                userId, AuthSupport.isAdmin(authentication), request);
            log.info("Payment processed successfully: {}", paymentDTO.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(paymentDTO);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error processing payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/validate")
    public ResponseEntity<PaymentDTO> validatePayment(
        @Valid @RequestBody PaymentValidationRequest request,
        Authentication authentication) {
        try {
            Long userId = requireUserId(authentication);
            log.info("Validating payment: {}", request.getRazorpayOrderId());
            PaymentDTO paymentDTO = paymentService.validatePayment(
                userId, AuthSupport.isAdmin(authentication), request);
            log.info("Payment validated successfully: {}", paymentDTO.getId());
            return ResponseEntity.ok(paymentDTO);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Payment validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error validating payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/methods")
    public ResponseEntity<List<PaymentMethodDTO>> getPaymentMethods(Authentication authentication) {
        try {
            Long userId = requireUserId(authentication);
            log.info("Fetching payment methods for user: {}", userId);
            List<PaymentMethodDTO> methods = paymentService.getUserPaymentMethods(userId);
            return ResponseEntity.ok(methods);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching payment methods", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/methods")
    public ResponseEntity<PaymentMethodDTO> addPaymentMethod(
        @Valid @RequestBody PaymentMethod paymentMethod,
        Authentication authentication) {
        try {
            Long userId = requireUserId(authentication);
            log.info("Adding payment method for user: {}", userId);
            PaymentMethodDTO methodDTO = paymentService.addPaymentMethod(userId, paymentMethod);
            log.info("Payment method added: {}", methodDTO.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(methodDTO);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment method: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error adding payment method", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @DeleteMapping("/methods/{id}")
    public ResponseEntity<Void> removePaymentMethod(
        @PathVariable Long id,
        Authentication authentication) {
        try {
            Long userId = requireUserId(authentication);
            log.info("Removing payment method: {} for user: {}", id, userId);
            paymentService.removePaymentMethod(userId, id);
            log.info("Payment method removed: {}", id);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            throw e;
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
    
    @GetMapping("/receipt/{bookingId}")
    public ResponseEntity<ReceiptDTO> getReceipt(
        @PathVariable Long bookingId,
        Authentication authentication) {
        try {
            Long userId = requireUserId(authentication);
            log.info("Fetching receipt for booking: {}", bookingId);
            ReceiptDTO receiptDTO = paymentService.getReceiptForBooking(
                userId, AuthSupport.isAdmin(authentication), bookingId);
            return ResponseEntity.ok(receiptDTO);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Receipt not found for booking: {}", bookingId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching receipt", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/refund")
    public ResponseEntity<RefundDTO> processRefund(
        @Valid @RequestBody RefundRequest request,
        Authentication authentication) {
        try {
            Long userId = requireUserId(authentication);
            log.info("Processing refund for payment: {}", request.getPaymentId());
            RefundDTO refundDTO = paymentService.processRefund(userId, request);
            log.info("Refund processed: {}", refundDTO.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(refundDTO);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Refund request error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error processing refund", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/transaction/{id}")
    public ResponseEntity<TransactionDTO> getTransaction(
        @PathVariable Long id,
        Authentication authentication) {
        try {
            Long userId = requireUserId(authentication);
            log.info("Fetching transaction details: {}", id);
            TransactionDTO transactionDTO = paymentService.getTransactionDetails(
                userId, AuthSupport.isAdmin(authentication), id);
            return ResponseEntity.ok(transactionDTO);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Transaction not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            log.error("Error fetching transaction", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/history")
    public ResponseEntity<Page<PaymentDTO>> getPaymentHistory(
        Pageable pageable,
        Authentication authentication) {
        try {
            Long userId = requireUserId(authentication);
            log.info("Fetching payment history for user: {}", userId);
            Page<PaymentDTO> history = paymentService.getPaymentHistory(userId, pageable);
            return ResponseEntity.ok(history);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching payment history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
        @RequestBody Map<String, Object> body,
        Authentication authentication) {
        try {
            Long userId = requireUserId(authentication);
            if (!body.containsKey("bookingId")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
            Long bookingId = Long.valueOf(body.get("bookingId").toString());
            String currency = body.getOrDefault("currency", "INR").toString();

            PaymentService.PaymentCheckoutOrderResult result = paymentService.createRazorpayOrder(
                userId, AuthSupport.isAdmin(authentication), currency, bookingId);

            Map<String, Object> resp = new HashMap<>();
            resp.put("orderId", result.orderId());
            resp.put("paymentId", result.paymentId());
            resp.put("amount", result.amount());
            resp.put("currency", result.currency());
            resp.put("keyId", paymentService.getRazorpayKeyId());
            return ResponseEntity.ok(resp);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Create order failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error creating Razorpay order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/webhook/razorpay")
    public ResponseEntity<Void> handleRazorpayWebhook(
        @RequestBody String payload,
        @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        try {
            log.info("Received Razorpay webhook event");
            paymentService.handleRazorpayWebhook(payload, signature);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Razorpay webhook rejected: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private static Long requireUserId(Authentication authentication) {
        Long userId = AuthSupport.userId(authentication);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return userId;
    }
}
