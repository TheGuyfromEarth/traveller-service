package com.travolish.traveller.notifications.dto;

import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationStatus;
import com.travolish.traveller.notifications.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private Long id;
    private Long userId;
    private NotificationType type;
    private NotificationChannel channel;
    private String subject;
    private String message;
    private Long templateId;
    private Long bookingId;
    private Long hotelId;
    private NotificationStatus status;
    private Boolean isRead;
    private Integer retryCount;
    private String errorMessage;
    private LocalDateTime scheduledTime;
    private LocalDateTime sentTime;
    private LocalDateTime readTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
