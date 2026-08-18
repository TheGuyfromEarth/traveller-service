package com.travolish.traveller.chat.service;

import com.travolish.traveller.chat.entity.Conversation;
import com.travolish.traveller.chat.entity.Message;
import com.travolish.traveller.chat.repository.ConversationRepository;
import com.travolish.traveller.chat.repository.MessageRepository;
import com.travolish.traveller.hosttools.entity.AutoReplyTemplate;
import com.travolish.traveller.hosttools.repository.AutoReplyTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Sends keyword-triggered auto-replies on behalf of a host when a guest message
 * matches one of the host's active template keywords. Runs asynchronously so
 * the guest's sendMessage POST returns immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HostAutoReplyService {

    private final AutoReplyTemplateRepository autoReplyTemplateRepository;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;

    @Async
    @Transactional
    public void triggerAutoReply(Long conversationId, Long guestSenderId, Long hostReceiverId, String messageText) {
        try {
            List<AutoReplyTemplate> templates = autoReplyTemplateRepository.findActiveByHostId(hostReceiverId);
            if (templates.isEmpty()) return;

            String lower = messageText.toLowerCase();
            AutoReplyTemplate match = templates.stream()
                .filter(t -> t.getTriggerKeyword() != null
                        && lower.contains(t.getTriggerKeyword().toLowerCase()))
                .findFirst()
                .orElse(null);
            if (match == null) return;

            Conversation conversation = conversationRepository.findById(conversationId).orElse(null);
            if (conversation == null) return;

            Message reply = new Message();
            reply.setConversation(conversation);
            reply.setSenderId(hostReceiverId);
            reply.setReceiverId(guestSenderId);
            reply.setMessageText(match.getTemplateText());
            reply.setIsRead(false);
            reply.setIsDeleted(false);
            messageRepository.save(reply);

            conversation.setLastMessageId(reply.getId());
            conversation.setLastMessageTime(LocalDateTime.now());
            if (guestSenderId.equals(conversation.getUserId1())) {
                conversation.setUser1UnreadCount(conversation.getUser1UnreadCount() + 1);
            } else {
                conversation.setUser2UnreadCount(conversation.getUser2UnreadCount() + 1);
            }
            conversationRepository.save(conversation);

            match.setUsageCount(match.getUsageCount() + 1);
            match.setLastUsedAt(LocalDateTime.now());
            autoReplyTemplateRepository.save(match);

            log.debug("HostAutoReplyService: auto-replied to conversation {} using template '{}'",
                conversationId, match.getTemplateName());
        } catch (Exception e) {
            log.warn("HostAutoReplyService: auto-reply failed for conversation {} ({}): {}",
                conversationId, e.getClass().getSimpleName(), e.getMessage());
        }
    }
}
