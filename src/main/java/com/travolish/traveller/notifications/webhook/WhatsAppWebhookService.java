package com.travolish.traveller.notifications.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travolish.traveller.notifications.entity.NotificationStatus;
import com.travolish.traveller.notifications.repository.NotificationRepository;
import com.travolish.traveller.notifications.whatsapp.WhatsAppConsentService;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookService {

    private static final List<String> OPT_OUT_KEYWORDS = List.of("STOP", "UNSUBSCRIBE", "CANCEL", "END", "QUIT");

    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;
    private final WhatsAppConsentService consentService;
    private final UserRepository userRepository;

    @Value("${whatsapp.meta.app-secret:}")
    private String appSecret;

    public boolean verifySignature(String payload, String signatureHeader) {
        if (appSecret == null || appSecret.isBlank()) {
            log.warn("WhatsApp app-secret not configured — rejecting webhook");
            return false;
        }
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String expected = signatureHeader.substring("sha256=".length());
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String computed = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return constantTimeEquals(expected, computed);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    public void handlePayload(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode entries = root.path("entry");
            if (!entries.isArray()) {
                return;
            }
            for (JsonNode entry : entries) {
                JsonNode changes = entry.path("changes");
                if (!changes.isArray()) {
                    continue;
                }
                for (JsonNode change : changes) {
                    JsonNode value = change.path("value");
                    processStatuses(value.path("statuses"));
                    processMessages(value.path("messages"));
                }
            }
        } catch (Exception e) {
            log.error("Failed to process WhatsApp webhook: {}", e.getMessage());
        }
    }

    private void processStatuses(JsonNode statuses) {
        if (!statuses.isArray()) {
            return;
        }
        for (JsonNode status : statuses) {
            String wamid = status.path("id").asText(null);
            String state = status.path("status").asText("");
            if (wamid == null) {
                continue;
            }
            notificationRepository.findByExternalMessageId(wamid).ifPresent(n -> {
                if ("delivered".equalsIgnoreCase(state) || "read".equalsIgnoreCase(state)) {
                    n.setStatus(NotificationStatus.DELIVERED);
                    notificationRepository.save(n);
                } else if ("failed".equalsIgnoreCase(state)) {
                    n.setStatus(NotificationStatus.FAILED);
                    n.setErrorMessage(status.path("errors").toString());
                    notificationRepository.save(n);
                }
            });
        }
    }

    private void processMessages(JsonNode messages) {
        if (!messages.isArray()) {
            return;
        }
        for (JsonNode message : messages) {
            String from = message.path("from").asText("");
            String text = message.path("text").path("body").asText("");
            if (text.isBlank()) {
                continue;
            }
            String normalized = text.trim().toUpperCase(Locale.ROOT);
            if (OPT_OUT_KEYWORDS.contains(normalized)) {
                log.info("WhatsApp opt-out received from ***{}", tail(from));
                String e164 = from.startsWith("+") ? from : "+" + from;
                userRepository.findByPhone(e164).ifPresent(user -> consentService.revoke(user.getId()));
            }
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static String tail(String digits) {
        if (digits.length() < 4) {
            return "****";
        }
        return digits.substring(digits.length() - 4);
    }
}
