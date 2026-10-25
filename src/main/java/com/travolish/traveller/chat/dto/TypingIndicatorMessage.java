package com.travolish.traveller.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorMessage {
    private Long conversationId;
    private Long userId;
    private String action; // "typing" or "stopped"
}
