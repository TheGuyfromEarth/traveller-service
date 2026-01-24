package com.travolish.traveller.chat.websocket;

import com.travolish.traveller.chat.dto.MessageDTO;
import com.travolish.traveller.chat.dto.SendMessageRequest;
import com.travolish.traveller.chat.dto.TypingIndicatorMessage;
import com.travolish.traveller.chat.dto.WebSocketMessage;
import com.travolish.traveller.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket message handler for real-time chat
 * Handles STOMP protocol messages for WebSocket communication
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {
    
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Handle sending messages via WebSocket
     * Client sends to: /app/chat/send
     * Message is broadcast to: /topic/chat/{conversationId}
     */
    @MessageMapping("/chat/send")
    public void handleSendMessage(@Payload SendMessageRequest request) {
        try {
            log.debug("Received message request for conversation: {}", request.getConversationId());
            
            // Save message to database
            MessageDTO messageDTO = chatService.sendMessage(
                request.getConversationId(),
                request.getConversationId(), // In production, get from authentication context
                request.getReceiverId(),
                request.getMessageText()
            );
            
            // Broadcast message to all subscribers of this conversation
            WebSocketMessage webSocketMessage = new WebSocketMessage();
            webSocketMessage.setType("message");
            webSocketMessage.setMessage(messageDTO);
            
            messagingTemplate.convertAndSend(
                "/topic/chat/" + request.getConversationId(),
                webSocketMessage
            );
            
            log.debug("Message sent successfully to conversation: {}", request.getConversationId());
        } catch (Exception e) {
            log.error("Error sending message", e);
            WebSocketMessage errorMessage = new WebSocketMessage();
            errorMessage.setType("error");
            errorMessage.getData().put("message", "Failed to send message: " + e.getMessage());
            messagingTemplate.convertAndSendToUser(
                request.getConversationId().toString(),
                "/queue/errors",
                errorMessage
            );
        }
    }
    
    /**
     * Handle typing indicator
     * Client sends to: /app/chat/typing
     * Message is broadcast to: /topic/chat/{conversationId}/typing
     */
    @MessageMapping("/chat/typing")
    public void handleTypingIndicator(@Payload TypingIndicatorMessage typingMessage) {
        try {
            log.debug("User {} is typing in conversation {}", 
                typingMessage.getUserId(), typingMessage.getConversationId());
            
            WebSocketMessage webSocketMessage = new WebSocketMessage();
            webSocketMessage.setType("typing");
            webSocketMessage.setTypingIndicator(typingMessage);
            
            messagingTemplate.convertAndSend(
                "/topic/chat/" + typingMessage.getConversationId() + "/typing",
                webSocketMessage
            );
        } catch (Exception e) {
            log.error("Error broadcasting typing indicator", e);
        }
    }
    
    /**
     * Handle marking message as read
     * Client sends to: /app/chat/read
     * Message is broadcast to: /topic/chat/{conversationId}/read
     */
    @MessageMapping("/chat/read/{conversationId}/{userId}")
    public void handleMarkAsRead(
            @DestinationVariable Long conversationId,
            @DestinationVariable Long userId) {
        try {
            log.debug("Marking messages as read for user {} in conversation {}", userId, conversationId);
            
            chatService.markMessagesAsRead(conversationId, userId);
            
            WebSocketMessage webSocketMessage = new WebSocketMessage();
            webSocketMessage.setType("read");
            webSocketMessage.getData().put("userId", userId);
            webSocketMessage.getData().put("conversationId", conversationId);
            
            messagingTemplate.convertAndSend(
                "/topic/chat/" + conversationId + "/read",
                webSocketMessage
            );
        } catch (Exception e) {
            log.error("Error marking messages as read", e);
        }
    }
    
    /**
     * Handle user coming online
     * Client sends to: /app/chat/online
     * Message is broadcast to: /topic/chat/{conversationId}/online
     */
    @MessageMapping("/chat/online/{conversationId}/{userId}")
    public void handleUserOnline(
            @DestinationVariable Long conversationId,
            @DestinationVariable Long userId) {
        try {
            log.debug("User {} is online in conversation {}", userId, conversationId);
            
            WebSocketMessage webSocketMessage = new WebSocketMessage();
            webSocketMessage.setType("online");
            webSocketMessage.getData().put("userId", userId);
            webSocketMessage.getData().put("conversationId", conversationId);
            webSocketMessage.getData().put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend(
                "/topic/chat/" + conversationId + "/online",
                webSocketMessage
            );
        } catch (Exception e) {
            log.error("Error handling user online status", e);
        }
    }
    
    /**
     * Handle user going offline
     * Client sends to: /app/chat/offline
     * Message is broadcast to: /topic/chat/{conversationId}/offline
     */
    @MessageMapping("/chat/offline/{conversationId}/{userId}")
    public void handleUserOffline(
            @DestinationVariable Long conversationId,
            @DestinationVariable Long userId) {
        try {
            log.debug("User {} is offline in conversation {}", userId, conversationId);
            
            WebSocketMessage webSocketMessage = new WebSocketMessage();
            webSocketMessage.setType("offline");
            webSocketMessage.getData().put("userId", userId);
            webSocketMessage.getData().put("conversationId", conversationId);
            webSocketMessage.getData().put("timestamp", System.currentTimeMillis());
            
            messagingTemplate.convertAndSend(
                "/topic/chat/" + conversationId + "/offline",
                webSocketMessage
            );
        } catch (Exception e) {
            log.error("Error handling user offline status", e);
        }
    }
}
