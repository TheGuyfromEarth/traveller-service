package com.travolish.traveller.payout.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.travolish.traveller.config.PlatformEconomicsProperties;
import com.travolish.traveller.kyc.entity.HostBankAccount;
import com.travolish.traveller.kyc.repository.HostBankAccountRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RazorpayXService {

    private final HostBankAccountRepository hostBankAccountRepository;
    private final PlatformEconomicsProperties economicsProperties;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${razorpay.x.enabled:false}")
    private boolean enabled;

    @Value("${razorpay.x.mode:test}")
    private String mode;

    @Value("${razorpay.x.account-number:}")
    private String razorpayXAccountNumber;

    @Value("${razorpay.x.key-id:}")
    private String razorpayXKeyId;

    @Value("${razorpay.x.key-secret:}")
    private String razorpayXKeySecret;

    @Value("${razorpay.api.key:}")
    private String fallbackKeyId;

    @Value("${razorpay.api.secret:}")
    private String fallbackKeySecret;

    public RazorpayXService(HostBankAccountRepository hostBankAccountRepository,
                            PlatformEconomicsProperties economicsProperties) {
        this.hostBankAccountRepository = hostBankAccountRepository;
        this.economicsProperties = economicsProperties;
    }

    public void ensureFundAccount(HostBankAccount bankAccount, Long hostId) {
        if (!enabled) {
            log.debug("Razorpay X disabled — skip fund account for bank {}", bankAccount.getId());
            return;
        }
        if (bankAccount.getRazorpayFundAccountId() != null && !bankAccount.getRazorpayFundAccountId().isBlank()) {
            return;
        }
        String ifsc = resolveIfsc(bankAccount);
        if (ifsc == null || ifsc.isBlank()) {
            log.warn("Cannot create Razorpay fund account for bank {} — missing IFSC", bankAccount.getId());
            return;
        }

        try {
            String contactId = bankAccount.getRazorpayContactId();
            if (contactId == null || contactId.isBlank()) {
                contactId = createContact(hostId, bankAccount);
                bankAccount.setRazorpayContactId(contactId);
            }
            String fundId = createFundAccount(contactId, bankAccount, ifsc);
            bankAccount.setRazorpayFundAccountId(fundId);
            bankAccount.setIfscCode(ifsc);
            hostBankAccountRepository.save(bankAccount);
            log.info("Razorpay fund account {} linked to bank {}", fundId, bankAccount.getId());
        } catch (Exception e) {
            log.error("Razorpay fund account setup failed for bank {}: {}", bankAccount.getId(), e.getMessage(), e);
        }
    }

    public String initiatePayoutTransfer(String fundAccountId, BigDecimal amountInr, String purpose) {
        if (!enabled) {
            String mockId = "pout_test_" + UUID.randomUUID().toString().substring(0, 12);
            log.warn("Razorpay X disabled — mock payout {} amount {}", mockId, amountInr);
            return mockId;
        }
        if (fundAccountId == null || fundAccountId.isBlank()) {
            throw new IllegalArgumentException("Razorpay fund account id is required");
        }
        if (razorpayXAccountNumber == null || razorpayXAccountNumber.isBlank()) {
            throw new IllegalStateException("razorpay.x.account-number is not configured");
        }

        long paise = amountInr.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        JSONObject body = new JSONObject();
        body.put("account_number", razorpayXAccountNumber);
        body.put("fund_account_id", fundAccountId);
        body.put("amount", paise);
        body.put("currency", economicsProperties.getCurrency());
        body.put("mode", economicsProperties.getPayoutMode());
        body.put("purpose", "payout");
        body.put("queue_if_low_balance", true);
        body.put("narration", purpose != null ? purpose : "Travolish host payout");

        try {
            JSONObject response = postJson("/v1/payouts", body);
            return response.getString("id");
        } catch (Exception e) {
            throw new IllegalStateException("Razorpay X payout failed: " + e.getMessage(), e);
        }
    }

    /** When X is disabled, mock payouts complete immediately in-process. */
    public boolean completesPayoutSynchronously() {
        return !enabled;
    }

    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            return com.razorpay.Utils.verifyWebhookSignature(payload, signature, webhookSecret());
        } catch (Exception e) {
            log.error("Razorpay X webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    private String webhookSecret() {
        return System.getenv().getOrDefault("RAZORPAY_X_WEBHOOK_SECRET",
            System.getenv().getOrDefault("RAZORPAY_WEBHOOK_SECRET", "dummy_webhook_secret"));
    }

    private String createContact(Long hostId, HostBankAccount bankAccount) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", bankAccount.getAccountHolderName());
        body.put("type", "vendor");
        body.put("reference_id", "host_" + hostId);
        JSONObject response = postJson("/v1/contacts", body);
        return response.getString("id");
    }

    private String createFundAccount(String contactId, HostBankAccount bankAccount, String ifsc) throws Exception {
        JSONObject bank = new JSONObject();
        bank.put("name", bankAccount.getAccountHolderName());
        bank.put("ifsc", ifsc);
        bank.put("account_number", bankAccount.getAccountNumber());

        JSONObject body = new JSONObject();
        body.put("contact_id", contactId);
        body.put("account_type", "bank_account");
        body.put("bank_account", bank);

        JSONObject response = postJson("/v1/fund_accounts", body);
        return response.getString("id");
    }

    private JSONObject postJson(String path, JSONObject body) throws Exception {
        String auth = Base64.getEncoder().encodeToString((apiKey() + ":" + apiSecret()).getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.razorpay.com" + path))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Razorpay X API " + path + " failed: " + response.statusCode() + " " + response.body());
        }
        return new JSONObject(response.body());
    }

    private String apiKey() {
        return razorpayXKeyId != null && !razorpayXKeyId.isBlank() ? razorpayXKeyId : fallbackKeyId;
    }

    private String apiSecret() {
        return razorpayXKeySecret != null && !razorpayXKeySecret.isBlank() ? razorpayXKeySecret : fallbackKeySecret;
    }

    private static String resolveIfsc(HostBankAccount bankAccount) {
        if (bankAccount.getIfscCode() != null && !bankAccount.getIfscCode().isBlank()) {
            return bankAccount.getIfscCode();
        }
        return bankAccount.getRoutingNumber();
    }
}
