package com.travolish.traveller.notifications.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travolish.traveller.notifications.repository.NotificationRepository;
import com.travolish.traveller.notifications.whatsapp.WhatsAppConsentService;
import com.travolish.traveller.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class WhatsAppWebhookServiceTest {

    private WhatsAppWebhookService service;

    @BeforeEach
    void setUp() {
        service = new WhatsAppWebhookService(
                new ObjectMapper(),
                mock(NotificationRepository.class),
                mock(WhatsAppConsentService.class),
                mock(UserRepository.class));
        ReflectionTestUtils.setField(service, "appSecret", "test-secret");
    }

    @Test
    void verifySignature_acceptsValidHmac() throws Exception {
        String payload = "{\"entry\":[]}";
        String sig = "sha256=" + hmac(payload, "test-secret");
        assertTrue(service.verifySignature(payload, sig));
    }

    @Test
    void verifySignature_rejectsInvalidHmac() {
        assertFalse(service.verifySignature("{}", "sha256=deadbeef"));
    }

    private static String hmac(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }
}
