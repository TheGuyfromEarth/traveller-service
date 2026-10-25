package com.travolish.traveller.chat.controller;

import com.travolish.traveller.chat.dto.ConversationDTO;
import com.travolish.traveller.chat.dto.MessageDTO;
import com.travolish.traveller.chat.dto.SendMessageRequest;
import com.travolish.traveller.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST API endpoints for chat functionality
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    
    /**
     * List all conversations for the current user
     * GET /api/chat/conversations
     */
    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDTO>> getConversations(@RequestParam Long userId) {
        List<ConversationDTO> conversations = chatService.getUserConversations(userId);
        return ResponseEntity.ok(conversations);
    }
    
    /**
     * Get conversation by ID
     * GET /api/chat/conversations/{id}
     */
    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationDTO> getConversation(@PathVariable Long id) {
        ConversationDTO conversation = chatService.getConversationById(id);
        return ResponseEntity.ok(conversation);
    }
    
    /**
     * Create or get existing conversation
     * POST /api/chat/conversations
     */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationDTO> createConversation(
            @RequestParam Long userId1,
            @RequestParam Long userId2) {
        ConversationDTO conversation = chatService.getOrCreateConversation(userId1, userId2);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }
    
    /**
     * Get conversation history (messages)
     * GET /api/chat/messages/{conversationId}
     */
    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<Page<MessageDTO>> getConversationHistory(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        Page<MessageDTO> messages = chatService.getConversationHistory(conversationId, page, pageSize);
        return ResponseEntity.ok(messages);
    }
    
    /**
     * Send a message
     * POST /api/chat/messages
     */
    @PostMapping("/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Long senderId = Long.parseLong(jwt.getSubject());
        MessageDTO message = chatService.sendMessage(
            request.getConversationId(),
            senderId,
            request.getReceiverId(),
            request.getMessageText()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }
    
    /**
     * Get unread message count for current user
     * GET /api/chat/conversations/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<Integer> getUnreadCount(@RequestParam Long userId) {
        Integer unreadCount = chatService.getUnreadCount(userId);
        return ResponseEntity.ok(unreadCount);
    }
    
    /**
     * Get unread count for a specific conversation
     * GET /api/chat/conversations/{conversationId}/unread
     */
    @GetMapping("/conversations/{conversationId}/unread")
    public ResponseEntity<Map<String, Object>> getConversationUnreadCount(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        Integer unreadCount = chatService.getConversationUnreadCount(conversationId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", conversationId);
        response.put("unreadCount", unreadCount);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Mark messages as read
     * PUT /api/chat/messages/{conversationId}/read
     */
    @PutMapping("/messages/{conversationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long conversationId,
            @RequestParam Long userId) {
        chatService.markMessagesAsRead(conversationId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", conversationId);
        response.put("message", "Messages marked as read");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Delete a message
     * DELETE /api/chat/messages/{id}
     */
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long id) {
        chatService.deleteMessage(id);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Message deleted successfully");
        return ResponseEntity.ok(response);
    }
}
