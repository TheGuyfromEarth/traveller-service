package com.travolish.traveller.notifications.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.travolish.traveller.notifications.whatsapp.WhatsAppTemplateCatalog;
import com.travolish.traveller.notifications.whatsapp.WhatsAppTemplateCatalog.TemplateDef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class WhatsAppService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${whatsapp.meta.access-token:}")
    private String accessToken;

    @Value("${whatsapp.meta.phone-number-id:}")
    private String phoneNumberId;

    @Value("${whatsapp.meta.api-version:v21.0}")
    private String apiVersion;

    public WhatsAppService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    public String sendTemplateForNotificationType(
            String toE164,
            com.travolish.traveller.notifications.entity.NotificationType type,
            Map<String, String> bodyParams) {
        TemplateDef def = WhatsAppTemplateCatalog.forType(type)
                .orElseThrow(() -> new IllegalArgumentException("No WhatsApp template for type " + type));
        return sendTemplateMessage(toE164, def.templateName(), def.languageCode(), bodyParams);
    }

    public String sendTemplateMessage(String toE164, String templateName, String languageCode, Map<String, String> bodyParams) {
        String to = toE164.replace("+", "");
        if (!enabled) {
            String mockId = "mock-wamid-" + UUID.randomUUID();
            log.info("WhatsApp disabled — mock send template={} to=***{}", templateName, tailDigits(to));
            return mockId;
        }
        if (accessToken.isBlank() || phoneNumberId.isBlank()) {
            throw new IllegalStateException("WhatsApp Meta credentials are not configured");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("messaging_product", "whatsapp");
        body.put("to", to);
        body.put("type", "template");

        ObjectNode template = body.putObject("template");
        template.put("name", templateName);
        template.putObject("language").put("code", languageCode);

        if (bodyParams != null && !bodyParams.isEmpty()) {
            ArrayNode parameters = objectMapper.createArrayNode();
            bodyParams.values().forEach(v -> parameters.addObject().put("type", "text").put("text", v));
            ObjectNode bodyComponent = objectMapper.createObjectNode();
            bodyComponent.put("type", "body");
            bodyComponent.set("parameters", parameters);
            template.set("components", objectMapper.createArrayNode().add(bodyComponent));
        }

        String url = "https://graph.facebook.com/" + apiVersion + "/" + phoneNumberId + "/messages";
        int attempts = 0;
        while (true) {
            attempts++;
            try {
                String response = restClient.post()
                        .uri(url)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body.toString())
                        .retrieve()
                        .body(String.class);

                String wamid = parseWamid(response);
                log.info("WhatsApp template={} wamid={} to=***{}", templateName, wamid, tailDigits(to));
                return wamid;
            } catch (RestClientResponseException ex) {
                if (ex.getStatusCode().value() == 429 && attempts < 4) {
                    sleepBackoff(attempts, ex.getResponseHeaders().getFirst(HttpHeaders.RETRY_AFTER));
                    continue;
                }
                log.error("WhatsApp API error status={} body={}", ex.getStatusCode().value(), ex.getResponseBodyAsString());
                throw new RuntimeException("WhatsApp send failed: " + ex.getMessage(), ex);
            }
        }
    }

    @Async("notificationExecutor")
    public void sendTemplateAsync(String toE164, String templateName, String languageCode, Map<String, String> bodyParams) {
        sendTemplateMessage(toE164, templateName, languageCode, bodyParams);
    }

    private String parseWamid(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode messages = root.path("messages");
            if (messages.isArray() && !messages.isEmpty()) {
                return messages.get(0).path("id").asText("unknown");
            }
        } catch (Exception e) {
            log.warn("Could not parse WhatsApp response wamid: {}", e.getMessage());
        }
        return "unknown";
    }

    private static void sleepBackoff(int attempt, String retryAfterHeader) {
        long ms = 500L * attempt * attempt;
        if (retryAfterHeader != null) {
            try {
                ms = Long.parseLong(retryAfterHeader) * 1000L;
            } catch (NumberFormatException ignored) {
            }
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String tailDigits(String digits) {
        if (digits == null || digits.length() < 4) {
            return "****";
        }
        return digits.substring(digits.length() - 4);
    }
}
