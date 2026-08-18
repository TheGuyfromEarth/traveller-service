package com.travolish.traveller.chat.service;

import com.travolish.traveller.chat.entity.Conversation;
import com.travolish.traveller.chat.entity.Message;
import com.travolish.traveller.chat.repository.ConversationRepository;
import com.travolish.traveller.chat.repository.MessageRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Generates AI-powered replies for the Travolish assistant using Google Gemini.
 * Runs asynchronously so the user's message POST returns immediately.
 */
@Slf4j
@Service
public class AssistantReplyService {

    private static final long SUPPORT_USER_ID = 4L;
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";
    private static final String SYSTEM_PROMPT =
            "You are Travolish AI, a friendly and knowledgeable travel assistant for the Travolish " +
            "hotel-booking platform. Help travellers with questions about stays, bookings, check-in " +
            "procedures, cancellations, refunds, payment issues, contacting hosts, reviews, amenities, " +
            "and trip planning. Be concise (2–4 sentences), warm, and actionable. " +
            "If a question is outside your scope, politely direct the user to our support team. " +
            "Never fabricate booking details or prices — refer the user to their Trips or " +
            "Transactions page for specifics.";

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String geminiApiKey;

    // Instantiated locally — no Spring bean required for a plain HTTP client
    private final RestTemplate restTemplate = new RestTemplate();

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;

    public AssistantReplyService(MessageRepository messageRepository,
                                 ConversationRepository conversationRepository) {
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
    }

    @PostConstruct
    void validateApiKey() {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is not set — AI chat will use fallback responses only. " +
                     "Set the environment variable GEMINI_API_KEY to enable live AI replies.");
        }
    }

    /**
     * Generates an AI reply for the given user message and persists it to the conversation.
     * Called asynchronously — the parent transaction has already committed by the time this runs.
     */
    @Async
    @Transactional
    public void generateAndSaveReply(Long conversationId, Long guestUserId, String userText) {
        try {
            String reply = callGemini(userText);

            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null) {
                log.error("Conversation {} not found — AI reply for guest {} cannot be saved.",
                          conversationId, guestUserId);
                return;
            }

            Message msg = new Message();
            msg.setConversation(conversation);
            msg.setSenderId(SUPPORT_USER_ID);
            msg.setReceiverId(guestUserId);
            msg.setMessageText(reply);
            msg.setIsRead(false);
            msg.setIsDeleted(false);
            messageRepository.save(msg);

            conversation.setLastMessageId(msg.getId());
            conversation.setLastMessageTime(LocalDateTime.now());
            if (guestUserId.equals(conversation.getUserId1())) {
                conversation.setUser1UnreadCount(conversation.getUser1UnreadCount() + 1);
            } else {
                conversation.setUser2UnreadCount(conversation.getUser2UnreadCount() + 1);
            }
            conversationRepository.save(conversation);

        } catch (Exception e) {
            log.error("Failed to generate/save AI reply for conversation {} ({}): {}",
                      conversationId, e.getClass().getSimpleName(), e.getMessage());
            // Attempt a single fallback save so the user receives some response even if
            // the primary flow failed (e.g. a DB timeout after the Gemini call).
            try {
                Conversation conv = conversationRepository.findById(conversationId).orElse(null);
                if (conv != null) {
                    Message fallbackMsg = new Message();
                    fallbackMsg.setConversation(conv);
                    fallbackMsg.setSenderId(SUPPORT_USER_ID);
                    fallbackMsg.setReceiverId(guestUserId);
                    fallbackMsg.setMessageText(fallback(userText));
                    fallbackMsg.setIsRead(false);
                    fallbackMsg.setIsDeleted(false);
                    messageRepository.save(fallbackMsg);
                    conv.setLastMessageId(fallbackMsg.getId());
                    conv.setLastMessageTime(LocalDateTime.now());
                    conversationRepository.save(conv);
                }
            } catch (Exception retryEx) {
                log.error("Fallback save also failed for conversation {}: {}",
                          conversationId, retryEx.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String callGemini(String userText) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            return fallback(userText);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> systemInstruction = Map.of(
                    "parts", List.of(Map.of("text", SYSTEM_PROMPT)));
            Map<String, Object> userContent = Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", userText)));
            Map<String, Object> body = Map.of(
                    "system_instruction", systemInstruction,
                    "contents", List.of(userContent),
                    "generationConfig", Map.of("maxOutputTokens", 300, "temperature", 0.7));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    GEMINI_URL + geminiApiKey, HttpMethod.POST, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                var candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    var content = (Map<String, Object>) candidates.get(0).get("content");
                    var parts = (List<Map<String, Object>>) content.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Gemini rate limit hit (429) — using fallback response. " +
                         "Free tier allows 15 RPM; consider upgrading if this recurs.");
            } else {
                log.warn("Gemini API HTTP {} error: {}", e.getStatusCode().value(), e.getMessage());
            }
        } catch (Exception e) {
            log.warn("Gemini API error ({}): {}", e.getClass().getSimpleName(), e.getMessage());
        }
        return fallback(userText);
    }

    private String fallback(String userText) {
        String t = userText.toLowerCase();
        if (t.contains("cancel"))
            return "For cancellations, open the booking from your Trips page and tap 'Cancel booking'. Refunds process within 5–7 business days per the property's policy.";
        if (t.contains("check-in") || t.contains("checkin") || t.contains("check in"))
            return "Check-in details are in your booking confirmation. Most hosts share access instructions 24 hours before arrival — check your Trips page.";
        if (t.contains("refund"))
            return "Refunds follow the property's cancellation policy and typically return to your original payment method within 5–7 business days.";
        if (t.contains("payment") || t.contains("charge") || t.contains("paid"))
            return "Check your Transactions page for full payment history. If you see an unexpected charge, share your booking ID and we'll look into it.";
        if (t.contains("host") || t.contains("contact"))
            return "You can message your host directly through the booking's chat thread on your Trips page. Hosts typically respond within a few hours.";
        if (t.contains("review") || t.contains("rating"))
            return "Leave a review for any completed stay from your Trips page. Reviews are published after a 48-hour window.";
        if (t.contains("wifi") || t.contains("wi-fi") || t.contains("internet"))
            return "Wi-Fi details are usually in the host's check-in instructions or listed under property amenities. You can also message your host directly.";
        return "Thanks for your question! I'm here to help with stays, bookings, cancellations, and trip planning. Our support team will follow up if you need more help.";
    }
}
