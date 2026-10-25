package com.travolish.traveller.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private Long id;
    private Long userId1;
    private Long userId2;
    private Long lastMessageId;
    private LocalDateTime lastMessageTime;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer user1UnreadCount;
    private Integer user2UnreadCount;
}
