package com.travolish.traveller.payment.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayIntegrationService {
    
    @Value("${razorpay.api.key:dummy_key_1234567890}")
    private String razorpayApiKey;

    @Value("${razorpay.api.secret:dummy_secret_1234567890}")
    private String razorpayApiSecret;

    /**
     * Webhook secret configured in the Razorpay dashboard under
     * Settings → Webhooks → Secret. Distinct from the API key/secret pair.
     */
    @Value("${razorpay.webhook.secret:dummy_webhook_secret}")
    private String razorpayWebhookSecret;

    private RazorpayClient razorpayClient;
    
    /**
     * Create Razorpay order for payment
     */
    public String createOrder(Long paymentId, BigDecimal amount, String currency, String description) {
        try {
            if (razorpayClient == null) {
                razorpayClient = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
            }
            
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amount.multiply(new BigDecimal(100)).longValue()); // Convert to paise
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "payment_" + paymentId);
            orderRequest.put("description", description);
            
            // Add notes
            JSONObject notes = new JSONObject();
            notes.put("payment_id", paymentId);
            orderRequest.put("notes", notes);
            
            // Create order
            com.razorpay.Order order = razorpayClient.orders.create(orderRequest);
            String orderId = (String) order.get("id");
            
            log.info("Razorpay order created: {} for payment: {}", orderId, paymentId);
            return orderId;
            
        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order for payment {}: {}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Failed to create Razorpay order: " + e.getMessage());
        }
    }
    
    /**
     * Fetch order details from Razorpay
     */
    public JSONObject getOrderDetails(String orderId) {
        try {
            if (razorpayClient == null) {
                razorpayClient = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
            }
            
            com.razorpay.Order order = razorpayClient.orders.fetch(orderId);
            return new JSONObject(order);
            
        } catch (RazorpayException e) {
            log.error("Error fetching Razorpay order {}: {}", orderId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch order details: " + e.getMessage());
        }
    }
    
    /**
     * Fetch payment details from Razorpay
     */
    public JSONObject getPaymentDetails(String paymentId) {
        try {
            if (razorpayClient == null) {
                razorpayClient = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
            }
            
            com.razorpay.Payment payment = razorpayClient.payments.fetch(paymentId);
            return new JSONObject(payment);
            
        } catch (RazorpayException e) {
            log.error("Error fetching Razorpay payment {}: {}", paymentId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch payment details: " + e.getMessage());
        }
    }
    
    /**
     * Process refund through Razorpay
     */
    public String processRefund(String razorpayPaymentId, BigDecimal refundAmount) {
        try {
            if (razorpayClient == null) {
                razorpayClient = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
            }
            
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", refundAmount.multiply(new BigDecimal(100)).longValue()); // Convert to paise
            
            com.razorpay.Refund refund = razorpayClient.payments.refund(razorpayPaymentId, refundRequest);
            String refundId = (String) refund.get("id");
            
            log.info("Refund processed: {} for Razorpay payment: {}", refundId, razorpayPaymentId);
            return refundId;
            
        } catch (RazorpayException e) {
            log.error("Error processing refund for payment {}: {}", razorpayPaymentId, e.getMessage(), e);
            throw new RuntimeException("Failed to process refund: " + e.getMessage());
        }
    }
    
    /**
     * Get refund details from Razorpay
     */
    public JSONObject getRefundDetails(String refundId) {
        try {
            if (razorpayClient == null) {
                razorpayClient = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
            }
            
            com.razorpay.Refund refund = razorpayClient.refunds.fetch(refundId);
            return new JSONObject(refund);
            
        } catch (RazorpayException e) {
            log.error("Error fetching Razorpay refund {}: {}", refundId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch refund details: " + e.getMessage());
        }
    }
    
    /**
     * Verify payment signature
     */
    public boolean verifyPaymentSignature(String orderId, String paymentId, String signature) {
        try {
            // Construct the string to verify
            String data = orderId + "|" + paymentId;
            
            // Verify using Razorpay utility
            return com.razorpay.Utils.verifySignature(data, signature, razorpayApiSecret);
            
        } catch (Exception e) {
            log.error("Error verifying payment signature: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Verifies the HMAC-SHA256 signature Razorpay attaches to every webhook POST.
     * The signature is computed over the raw request body using the webhook secret
     * configured in the Razorpay dashboard (Settings → Webhooks → Secret), which is
     * separate from the API key/secret pair used for regular API calls.
     *
     * <p>Reject any webhook where this returns false — an invalid signature means the
     * request did not originate from Razorpay and should not be processed.
     */
    public boolean verifyWebhookSignature(String payload, String receivedSignature) {
        try {
            return com.razorpay.Utils.verifyWebhookSignature(payload, receivedSignature, razorpayWebhookSecret);
        } catch (Exception e) {
            log.error("Error verifying Razorpay webhook signature: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Initiates a host payout via the Razorpay Payouts API (Razorpay X / Current Account).
     *
     * <p>Requires a Razorpay X current account and a pre-created fund account for the
     * host's bank details. Once Razorpay X is configured, replace the body of this
     * method with a real {@code razorpayClient.payouts.create(request)} call.
     *
     * @param fundAccountId  Razorpay fund_account_id for the host's bank account
     * @param amount         Net amount to transfer (in INR)
     * @param currency       Currency code (e.g. "INR")
     * @param purpose        Human-readable description stored on the payout
     * @return               Razorpay payout ID (e.g. "pout_Abcd1234")
     * @throws UnsupportedOperationException until Razorpay X is configured
     */
    public String initiatePayoutTransfer(String fundAccountId, java.math.BigDecimal amount,
                                         String currency, String purpose) {
        // TODO(payout): Replace with razorpayClient.payouts.create(request) once
        //   Razorpay X / Current Account is set up and RAZORPAY_X_KEY / RAZORPAY_X_SECRET
        //   are provisioned. See https://razorpay.com/docs/razorpayx/api/payouts/
        log.warn("Razorpay payout transfer not yet configured — fund_account={}, amount={} {}",
                fundAccountId, amount, currency);
        throw new UnsupportedOperationException(
                "Payout gateway not configured. Set up Razorpay X and implement initiatePayoutTransfer().");
    }

    /**
     * Create Razorpay customer for recurring payments
     */
    public String createCustomer(String email, String contactNumber, String name) {
        try {
            if (razorpayClient == null) {
                razorpayClient = new RazorpayClient(razorpayApiKey, razorpayApiSecret);
            }
            
            JSONObject customerRequest = new JSONObject();
            customerRequest.put("email", email);
            customerRequest.put("contact", contactNumber);
            customerRequest.put("name", name);
            
            com.razorpay.Customer customer = razorpayClient.customers.create(customerRequest);
            String customerId = (String) customer.get("id");
            
            log.info("Razorpay customer created: {} for email: {}", customerId, email);
            return customerId;
            
        } catch (RazorpayException e) {
            log.error("Error creating Razorpay customer: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create customer: " + e.getMessage());
        }
    }
}
