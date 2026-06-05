package com.travolish.traveller.notifications.controller;

import com.travolish.traveller.notifications.dto.NotificationDTO;
import com.travolish.traveller.notifications.dto.NotificationTemplateDTO;
import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.dto.UserNotificationPreferenceDTO;
import com.travolish.traveller.notifications.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Notification management APIs")
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Send notification (Email or SMS)
     * POST /api/notifications/email
     * POST /api/notifications/sms
     */
    @PostMapping("/email")
    @Operation(summary = "Send email notification", description = "Send an email notification to a user")
    public ResponseEntity<?> sendEmailNotification(@RequestBody SendNotificationRequest request) {
        try {
            log.info("Sending email notification to user: {}", request.getUserId());
            NotificationDTO notification = notificationService.sendNotification(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(notification);
        } catch (Exception e) {
            log.error("Error sending email notification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error sending notification: " + e.getMessage());
        }
    }
    
    @PostMapping("/sms")
    @Operation(summary = "Send SMS notification", description = "Send an SMS notification to a user")
    public ResponseEntity<?> sendSmsNotification(@RequestBody SendNotificationRequest request) {
        try {
            log.info("Sending SMS notification to user: {}", request.getUserId());
            NotificationDTO notification = notificationService.sendNotification(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(notification);
        } catch (Exception e) {
            log.error("Error sending SMS notification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error sending notification: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/notifications/templates
     * Get all notification templates
     */
    @GetMapping("/templates")
    @Operation(summary = "Get notification templates", description = "Retrieve all available notification templates")
    public ResponseEntity<List<NotificationTemplateDTO>> getTemplates() {
        try {
            log.info("Fetching all notification templates");
            List<NotificationTemplateDTO> templates = notificationService.getAllTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            log.error("Error fetching templates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/notifications/templates/active
     * Get active notification templates
     */
    @GetMapping("/templates/active")
    @Operation(summary = "Get active templates", description = "Retrieve active notification templates only")
    public ResponseEntity<List<NotificationTemplateDTO>> getActiveTemplates() {
        try {
            log.info("Fetching active notification templates");
            List<NotificationTemplateDTO> templates = notificationService.getActiveTemplates();
            return ResponseEntity.ok(templates);
        } catch (Exception e) {
            log.error("Error fetching active templates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * POST /api/notifications/schedule
     * Schedule notification for later
     */
    @PostMapping("/schedule")
    @Operation(summary = "Schedule notification", description = "Schedule a notification to be sent at a specific time")
    public ResponseEntity<?> scheduleNotification(@RequestBody SendNotificationRequest request) {
        try {
            request.setSendImmediately(false);
            log.info("Scheduling notification for user: {} at time: {}", request.getUserId(), request.getScheduledTime());
            NotificationDTO notification = notificationService.sendNotification(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(notification);
        } catch (Exception e) {
            log.error("Error scheduling notification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error scheduling notification: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/notifications/user/{userId}
     * Get user notifications
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user notifications", description = "Retrieve paginated notifications for a specific user")
    @Parameter(name = "page", description = "Page number (0-indexed)", example = "0")
    @Parameter(name = "size", description = "Page size", example = "10")
    public ResponseEntity<Page<NotificationDTO>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String email) {
        try {
            log.info("Fetching notifications for user: {} email: {} (page: {}, size: {})", userId, email, page, size);
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationDTO> notifications = notificationService.getUserNotificationsWithEmailFallback(userId, email, pageable);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Error fetching user notifications: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/notifications/user/{userId}/unread
     * Get unread notifications
     */
    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Get unread notifications", description = "Retrieve unread notifications for a user")
    public ResponseEntity<Page<NotificationDTO>> getUnreadNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("Fetching unread notifications for user: {}", userId);
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId, pageable);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            log.error("Error fetching unread notifications: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * GET /api/notifications/user/{userId}/unread-count
     * Get unread count
     */
    @GetMapping("/user/{userId}/unread-count")
    @Operation(summary = "Get unread count", description = "Get the count of unread notifications for a user")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long userId) {
        try {
            log.info("Fetching unread count for user: {}", userId);
            Long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            log.error("Error fetching unread count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * POST /api/notifications/{notificationId}/read
     * Mark notification as read
     */
    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        try {
            log.info("Marking notification as read: {}", notificationId);
            NotificationDTO notification = notificationService.markAsRead(notificationId);
            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Notification not found: " + e.getMessage());
        }
    }
    
    /**
     * DELETE /api/notifications/{notificationId}
     * Delete notification
     */
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete notification", description = "Delete a notification")
    public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId) {
        try {
            log.info("Deleting notification: {}", notificationId);
            notificationService.deleteNotification(notificationId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting notification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Notification not found: " + e.getMessage());
        }
    }
    
    /**
     * GET /api/notifications/preferences/user/{userId}
     * Get user notification preferences
     */
    @GetMapping("/preferences/user/{userId}")
    @Operation(summary = "Get notification preferences", description = "Get notification preferences for a user")
    public ResponseEntity<?> getUserPreferences(@PathVariable Long userId) {
        try {
            log.info("Fetching preferences for user: {}", userId);
            UserNotificationPreferenceDTO preferences = notificationService.getUserPreferences(userId);
            return ResponseEntity.ok(preferences);
        } catch (Exception e) {
            log.error("Error fetching user preferences: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Preferences not found: " + e.getMessage());
        }
    }
    
    /**
     * PUT /api/notifications/preferences/user/{userId}
     * Update user notification preferences
     */
    @PutMapping("/preferences/user/{userId}")
    @Operation(summary = "Update preferences", description = "Update notification preferences for a user")
    public ResponseEntity<?> updateUserPreferences(
            @PathVariable Long userId,
            @RequestBody UserNotificationPreferenceDTO dto) {
        try {
            log.info("Updating preferences for user: {}", userId);
            UserNotificationPreferenceDTO updated = notificationService.updateUserPreferences(userId, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating user preferences: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Error updating preferences: " + e.getMessage());
        }
    }
}
