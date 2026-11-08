package com.travolish.traveller.notifications.controller;

import com.travolish.traveller.notifications.dto.NotificationDTO;
import com.travolish.traveller.notifications.dto.NotificationTemplateDTO;
import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.dto.UserNotificationPreferenceDTO;
import com.travolish.traveller.notifications.service.NotificationService;
import com.travolish.traveller.user.security.AuthSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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

    @PostMapping("/email")
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
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

    @GetMapping("/templates")
    @Operation(summary = "Get notification templates", description = "Retrieve all available notification templates")
    public ResponseEntity<List<NotificationTemplateDTO>> getTemplates() {
        try {
            return ResponseEntity.ok(notificationService.getAllTemplates());
        } catch (Exception e) {
            log.error("Error fetching templates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/templates/active")
    @Operation(summary = "Get active templates", description = "Retrieve active notification templates only")
    public ResponseEntity<List<NotificationTemplateDTO>> getActiveTemplates() {
        try {
            return ResponseEntity.ok(notificationService.getActiveTemplates());
        } catch (Exception e) {
            log.error("Error fetching active templates: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Schedule notification", description = "Schedule a notification to be sent at a specific time")
    public ResponseEntity<?> scheduleNotification(@RequestBody SendNotificationRequest request) {
        try {
            request.setSendImmediately(false);
            NotificationDTO notification = notificationService.sendNotification(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(notification);
        } catch (Exception e) {
            log.error("Error scheduling notification: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Error scheduling notification: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user notifications", description = "Retrieve paginated notifications for a specific user")
    @Parameter(name = "page", description = "Page number (0-indexed)", example = "0")
    @Parameter(name = "size", description = "Page size", example = "10")
    public ResponseEntity<Page<NotificationDTO>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String email,
            Authentication authentication) {
        try {
            AuthSupport.requireSelfOrAdmin(authentication, userId);
            Pageable pageable = PageRequest.of(page, size);
            Page<NotificationDTO> notifications =
                    notificationService.getUserNotificationsWithEmailFallback(userId, email, pageable);
            return ResponseEntity.ok(notifications);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error fetching user notifications: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/user/{userId}/unread")
    @Operation(summary = "Get unread notifications", description = "Retrieve unread notifications for a user")
    public ResponseEntity<Page<NotificationDTO>> getUnreadNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        AuthSupport.requireSelfOrAdmin(authentication, userId);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getUnreadNotifications(userId, pageable));
    }

    @GetMapping("/user/{userId}/unread-count")
    @Operation(summary = "Get unread count", description = "Get the count of unread notifications for a user")
    public ResponseEntity<Long> getUnreadCount(@PathVariable Long userId, Authentication authentication) {
        AuthSupport.requireSelfOrAdmin(authentication, userId);
        return ResponseEntity.ok(notificationService.getUnreadCount(userId));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Mark as read", description = "Mark a notification as read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId, Authentication authentication) {
        Long userId = AuthSupport.requireUserId(authentication);
        boolean admin = AuthSupport.isAdmin(authentication);
        return ResponseEntity.ok(notificationService.markAsRead(notificationId, userId, admin));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete notification", description = "Delete a notification")
    public ResponseEntity<?> deleteNotification(@PathVariable Long notificationId,
            Authentication authentication) {
        Long userId = AuthSupport.requireUserId(authentication);
        boolean admin = AuthSupport.isAdmin(authentication);
        notificationService.deleteNotification(notificationId, userId, admin);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences/user/{userId}")
    @Operation(summary = "Get notification preferences", description = "Get notification preferences for a user")
    public ResponseEntity<?> getUserPreferences(@PathVariable Long userId, Authentication authentication) {
        AuthSupport.requireSelfOrAdmin(authentication, userId);
        return ResponseEntity.ok(notificationService.getUserPreferences(userId));
    }

    @PutMapping("/preferences/user/{userId}")
    @Operation(summary = "Update preferences", description = "Update notification preferences for a user")
    public ResponseEntity<?> updateUserPreferences(
            @PathVariable Long userId,
            @RequestBody UserNotificationPreferenceDTO dto,
            Authentication authentication) {
        AuthSupport.requireSelfOrAdmin(authentication, userId);
        return ResponseEntity.ok(notificationService.updateUserPreferences(userId, dto));
    }
}
