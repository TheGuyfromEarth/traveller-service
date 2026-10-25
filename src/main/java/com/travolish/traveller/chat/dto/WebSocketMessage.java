package com.travolish.traveller.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketMessage {
    private String type; // "message", "typing", "read", "online", etc.
    private MessageDTO message;
    private TypingIndicatorMessage typingIndicator;
    private Map<String, Object> data = new HashMap<>();
}
