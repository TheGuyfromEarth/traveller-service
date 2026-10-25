package com.travolish.traveller.notifications.dto;

import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendNotificationRequest {
    private Long userId;
    private NotificationType type;
    private NotificationChannel channel; // EMAIL, SMS, IN_APP
    private String recipientEmail;
    private String recipientPhone;
    private String subject; // If not using template
    private String message; // If not using template
    private Long templateId; // If using template
    private Map<String, String> templateVariables; // Variables for template substitution
    private Long bookingId; // Optional context
    private Long hotelId; // Optional context
    private LocalDateTime scheduledTime; // Optional: if scheduling for later
    private Boolean sendImmediately; // Default: true
}
