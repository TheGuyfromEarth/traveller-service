package com.travolish.traveller.chat.service;

import com.travolish.traveller.chat.dto.ConversationDTO;
import com.travolish.traveller.chat.dto.MessageDTO;
import com.travolish.traveller.chat.entity.Conversation;
import com.travolish.traveller.chat.entity.Message;
import com.travolish.traveller.chat.repository.ConversationRepository;
import com.travolish.traveller.chat.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ModelMapper modelMapper;
    
    // Conversation Methods
    
    /**
     * Get or create a conversation between two users
     */
    public ConversationDTO getOrCreateConversation(Long userId1, Long userId2) {
        Conversation conversation = conversationRepository.findConversation(userId1, userId2)
            .orElseGet(() -> {
                Conversation newConversation = new Conversation();
                newConversation.setUserId1(userId1);
                newConversation.setUserId2(userId2);
                newConversation.setIsActive(true);
                newConversation.setUser1UnreadCount(0);
                newConversation.setUser2UnreadCount(0);
                return conversationRepository.save(newConversation);
            });
        return modelMapper.map(conversation, ConversationDTO.class);
    }
    
    /**
     * Get all conversations for a user
     */
    public List<ConversationDTO> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findUserConversations(userId);
        return conversations.stream()
            .map(c -> modelMapper.map(c, ConversationDTO.class))
            .collect(Collectors.toList());
    }
    
    /**
     * Get conversation by ID
     */
    public ConversationDTO getConversationById(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        return modelMapper.map(conversation, ConversationDTO.class);
    }
    
    /**
     * Get conversations with unread messages
     */
    public List<ConversationDTO> getUnreadConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findConversationsWithUnreadMessages(userId);
        return conversations.stream()
            .map(c -> modelMapper.map(c, ConversationDTO.class))
            .collect(Collectors.toList());
    }
    
    // Message Methods
    
    /**
     * Send a message
     */
    public MessageDTO sendMessage(Long conversationId, Long senderId, Long receiverId, String messageText) {
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageText(messageText);
        message.setIsRead(false);
        message.setIsDeleted(false);
        
        message = messageRepository.save(message);
        
        // Update conversation's last message info
        conversation.setLastMessageId(message.getId());
        conversation.setLastMessageTime(LocalDateTime.now());
        
        // Increment unread count for receiver
        if (receiverId.equals(conversation.getUserId1())) {
            conversation.setUser1UnreadCount(conversation.getUser1UnreadCount() + 1);
        } else {
            conversation.setUser2UnreadCount(conversation.getUser2UnreadCount() + 1);
        }
        
        conversationRepository.save(conversation);
        
        return modelMapper.map(message, MessageDTO.class);
    }
    
    /**
     * Get message history for a conversation (paginated)
     */
    public Page<MessageDTO> getConversationHistory(Long conversationId, int page, int pageSize) {
        Page<Message> messages = messageRepository.findByConversationIdPaged(
            conversationId, 
            PageRequest.of(page, pageSize)
        );
        return messages.map(m -> modelMapper.map(m, MessageDTO.class));
    }
    
    /**
     * Get all unread message count for a user
     */
    public Integer getUnreadCount(Long userId) {
        List<Conversation> conversations = conversationRepository.findUserConversations(userId);
        int totalUnread = 0;
        
        for (Conversation conversation : conversations) {
            if (userId.equals(conversation.getUserId1())) {
                totalUnread += conversation.getUser1UnreadCount();
            } else {
                totalUnread += conversation.getUser2UnreadCount();
            }
        }
        
        return totalUnread;
    }
    
    /**
     * Mark messages as read
     */
    public void markMessagesAsRead(Long conversationId, Long userId) {
        List<Message> unreadMessages = messageRepository.findUnreadMessages(conversationId, userId);
        
        for (Message message : unreadMessages) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            messageRepository.save(message);
        }
        
        // Update conversation's unread count
        Conversation conversation = conversationRepository.findById(conversationId)
            .orElseThrow(() -> new RuntimeException("Conversation not found"));
        
        if (userId.equals(conversation.getUserId1())) {
            conversation.setUser1UnreadCount(0);
        } else {
            conversation.setUser2UnreadCount(0);
        }
        
        conversationRepository.save(conversation);
    }
    
    /**
     * Delete a message
     */
    public void deleteMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("Message not found"));
        
        message.setIsDeleted(true);
        messageRepository.save(message);
    }
    
    /**
     * Get unread count for a specific conversation
     */
    public Integer getConversationUnreadCount(Long conversationId, Long userId) {
        return messageRepository.countUnreadMessages(conversationId, userId);
    }
}
