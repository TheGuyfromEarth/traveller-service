package com.travolish.traveller.notifications.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travolish.traveller.notifications.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WhatsAppServiceTest {

    @Test
    void sendTemplate_whenDisabled_returnsMockWamid() {
        WhatsAppService service = new WhatsAppService(new ObjectMapper());
        ReflectionTestUtils.setField(service, "enabled", false);

        String wamid = service.sendTemplateForNotificationType(
                "+919876543210",
                NotificationType.WELCOME,
                Map.of("1", "Travolish"));

        assertTrue(wamid.startsWith("mock-wamid-"));
    }
}
