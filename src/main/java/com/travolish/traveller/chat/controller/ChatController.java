package com.travolish.traveller.chat.controller;

import com.travolish.traveller.chat.dto.ConversationDTO;
import com.travolish.traveller.chat.dto.MessageDTO;
import com.travolish.traveller.chat.dto.SendMessageRequest;
import com.travolish.traveller.chat.service.ChatService;
import com.travolish.traveller.user.security.AuthSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationDTO>> getConversations(Authentication authentication) {
        Long userId = AuthSupport.requireUserId(authentication);
        return ResponseEntity.ok(chatService.getUserConversations(userId));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ConversationDTO> getConversation(@PathVariable Long id,
            Authentication authentication) {
        Long userId = AuthSupport.requireUserId(authentication);
        return ResponseEntity.ok(chatService.getConversationById(id, userId));
    }

    @PostMapping("/conversations")
    public ResponseEntity<ConversationDTO> createConversation(
            @RequestParam Long otherUserId,
            Authentication authentication) {
        Long userId = AuthSupport.requireUserId(authentication);
        if (Objects.equals(userId, otherUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot chat with yourself");
        }
        ConversationDTO conversation = chatService.getOrCreateConversation(userId, otherUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    @GetMapping("/messages/{conversationId}")
    public ResponseEntity<Page<MessageDTO>> getConversationHistory(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int pageSize,
            Authentication authentication) {
        Long userId = AuthSupport.requireUserId(authentication);
        return ResponseEntity.ok(chatService.getConversationHistory(conversationId, userId, page, pageSize));
    }

    @PostMapping("/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @RequestBody SendMessageRequest request,
            Authentication authentication) {
        Long senderId = AuthSupport.requireUserId(authentication);
        MessageDTO message = chatService.sendMessage(
            request.getConversationId(),
            senderId,
            request.getReceiverId(),
            request.getMessageText()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(message);
    }

    @GetMapping("/unread")
    public ResponseEntity<Integer> getUnreadCount(Authentication authentication) {
        Long userId = AuthSupport.requireUserId(authentication);
        return ResponseEntity.ok(chatService.getUnreadCount(userId));
    }

    @GetMapping("/conversations/{conversationId}/unread")
    public ResponseEntity<Map<String, Object>> getConversationUnreadCount(
            @PathVariable Long conversationId,
            Authentication authentication) {
        Long userId = AuthSupport.requireUserId(authentication);
        Integer unreadCount = chatService.getConversationUnreadCount(conversationId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", conversationId);
        response.put("unreadCount", unreadCount);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/messages/{conversationId}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(
            @PathVariable Long conversationId,
            Authentication authentication) {
        Long userId = AuthSupport.requireUserId(authentication);
        chatService.markMessagesAsRead(conversationId, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("conversationId", conversationId);
        response.put("message", "Messages marked as read");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Map<String, Object>> deleteMessage(@PathVariable Long id,
            Authentication authentication) {
        Long userId = AuthSupport.requireUserId(authentication);
        chatService.deleteMessage(id, userId);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Message deleted successfully");
        return ResponseEntity.ok(response);
    }
}
