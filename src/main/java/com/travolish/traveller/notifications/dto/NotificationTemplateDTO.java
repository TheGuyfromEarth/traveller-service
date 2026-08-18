package com.travolish.traveller.notifications.dto;

import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationTemplateDTO {
    private Long id;
    private NotificationType type;
    private String name;
    private String description;
    private String subjectTemplate;
    private String messageTemplate;
    private String htmlTemplate;
    private NotificationChannel channel;
    private Boolean isActive;
    private Boolean retryEnabled;
    private Integer maxRetries;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
