package com.travolish.traveller.notifications.service;

import com.travolish.traveller.notifications.dto.NotificationDTO;
import org.springframework.data.domain.PageImpl;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import com.travolish.traveller.notifications.dto.NotificationTemplateDTO;
import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.dto.UserNotificationPreferenceDTO;
import com.travolish.traveller.notifications.entity.*;
import com.travolish.traveller.notifications.exception.NotificationException;
import com.travolish.traveller.notifications.exception.NotificationNotFoundException;
import com.travolish.traveller.notifications.repository.NotificationRepository;
import com.travolish.traveller.notifications.repository.NotificationTemplateRepository;
import com.travolish.traveller.notifications.repository.UserNotificationPreferenceRepository;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final ModelMapper modelMapper;
    
    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationTemplateRepository templateRepository,
            UserNotificationPreferenceRepository preferenceRepository,
            EmailService emailService,
            SmsService smsService,
            ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.templateRepository = templateRepository;
        this.preferenceRepository = preferenceRepository;
        this.emailService = emailService;
        this.smsService = smsService;
        this.modelMapper = modelMapper;
    }
    
    /**
     * Fire-and-forget async wrapper used by internal callers (booking, user creation).
     * Failures are logged but never propagated to the calling thread.
     */
    @Async
    public void sendNotificationAsync(SendNotificationRequest request) {
        try {
            sendNotification(request);
        } catch (Exception e) {
            log.warn("Async notification failed: {}", e.getMessage());
        }
    }

    /**
     * Send notification via email or SMS — synchronous, returns the saved DTO.
     * Use sendNotificationAsync() for fire-and-forget callers.
     */
    public NotificationDTO sendNotification(SendNotificationRequest request) {
        try {
            // Check user preferences (skip when userId is unknown — e.g. guest bookings)
            if (request.getUserId() != null) {
                UserNotificationPreference preference = getOrCreateUserPreference(request.getUserId());
                if (!isNotificationAllowed(preference, request.getType())) {
                    log.info("Notification blocked by user preferences for userId: {}, type: {}",
                        request.getUserId(), request.getType());
                    return null;
                }
            }
            
            // Create notification entity
            Notification notification = createNotificationEntity(request);
            
            // Process based on channel
            if (request.getSendImmediately() == null || request.getSendImmediately()) {
                processNotification(notification, request);
            } else {
                notification.setStatus(NotificationStatus.SCHEDULED);
            }
            
            // Save and return
            Notification saved = notificationRepository.save(notification);
            return modelMapper.map(saved, NotificationDTO.class);
            
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage(), e);
            throw new NotificationException("Failed to send notification: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all notifications for a user
     */
    public Page<NotificationDTO> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserId(userId, pageable);
        return notifications.map(n -> modelMapper.map(n, NotificationDTO.class));
    }

    /**
     * Get notifications by userId + recipientEmail merged and deduplicated.
     * Handles legacy notifications where userId was not set (null) — found via email.
     */
    public Page<NotificationDTO> getUserNotificationsWithEmailFallback(Long userId, String email, Pageable pageable) {
        // Collect from both sources, deduplicate by id (userId-based first so they win)
        LinkedHashMap<Long, Notification> merged = new LinkedHashMap<>();
        notificationRepository.findByUserId(userId, pageable).forEach(n -> merged.put(n.getId(), n));
        if (email != null && !email.isBlank()) {
            notificationRepository.findByRecipientEmail(email)
                .forEach(n -> merged.putIfAbsent(n.getId(), n));
        }
        var list = new ArrayList<>(merged.values());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        var page = (start > list.size()) ? new ArrayList<Notification>() : list.subList(start, end);
        return new PageImpl<>(page, pageable, list.size())
            .map(n -> modelMapper.map(n, NotificationDTO.class));
    }
    
    /**
     * Get unread notifications for a user
     */
    public Page<NotificationDTO> getUnreadNotifications(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(userId, pageable);
        return notifications.map(n -> modelMapper.map(n, NotificationDTO.class));
    }
    
    /**
     * Mark notification as read
     */
    public NotificationDTO markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        
        notification.setIsRead(true);
        notification.setReadTime(LocalDateTime.now());
        
        Notification saved = notificationRepository.save(notification);
        return modelMapper.map(saved, NotificationDTO.class);
    }
    
    /**
     * Delete notification
     */
    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new NotificationNotFoundException(notificationId);
        }
        notificationRepository.deleteById(notificationId);
        log.info("Notification deleted with id: {}", notificationId);
    }
    
    /**
     * Get all notification templates
     */
    public List<NotificationTemplateDTO> getAllTemplates() {
        List<NotificationTemplate> templates = templateRepository.findAll();
        return templates.stream()
            .map(t -> modelMapper.map(t, NotificationTemplateDTO.class))
            .collect(Collectors.toList());
    }
    
    /**
     * Get active notification templates
     */
    public List<NotificationTemplateDTO> getActiveTemplates() {
        List<NotificationTemplate> templates = templateRepository.findByIsActiveTrue();
        return templates.stream()
            .map(t -> modelMapper.map(t, NotificationTemplateDTO.class))
            .collect(Collectors.toList());
    }
    
    /**
     * Get template by type
     */
    public NotificationTemplateDTO getTemplate(NotificationType type) {
        NotificationTemplate template = templateRepository.findByType(type)
            .orElseThrow(() -> new NotificationNotFoundException("Template not found for type: " + type));
        return modelMapper.map(template, NotificationTemplateDTO.class);
    }
    
    /**
     * Create or update notification template
     */
    public NotificationTemplateDTO createTemplate(NotificationTemplateDTO dto) {
        NotificationTemplate template = new NotificationTemplate();
        template.setType(dto.getType());
        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setSubjectTemplate(dto.getSubjectTemplate());
        template.setMessageTemplate(dto.getMessageTemplate());
        template.setHtmlTemplate(dto.getHtmlTemplate());
        template.setChannel(dto.getChannel());
        template.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        template.setRetryEnabled(dto.getRetryEnabled() != null ? dto.getRetryEnabled() : true);
        template.setMaxRetries(dto.getMaxRetries() != null ? dto.getMaxRetries() : 3);
        
        NotificationTemplate saved = templateRepository.save(template);
        return modelMapper.map(saved, NotificationTemplateDTO.class);
    }
    
    /**
     * Get user notification preferences — auto-creates with defaults if none exist
     */
    public UserNotificationPreferenceDTO getUserPreferences(Long userId) {
        UserNotificationPreference pref = getOrCreateUserPreference(userId);
        return modelMapper.map(pref, UserNotificationPreferenceDTO.class);
    }

    /**
     * Update user notification preferences — upserts; never overwrites userId
     */
    public UserNotificationPreferenceDTO updateUserPreferences(Long userId, UserNotificationPreferenceDTO dto) {
        UserNotificationPreference pref = getOrCreateUserPreference(userId);

        if (dto.getEmailEnabled() != null) pref.setEmailEnabled(dto.getEmailEnabled());
        if (dto.getSmsEnabled() != null) pref.setSmsEnabled(dto.getSmsEnabled());
        if (dto.getInAppEnabled() != null) pref.setInAppEnabled(dto.getInAppEnabled());
        if (dto.getBookingConfirmation() != null) pref.setBookingConfirmation(dto.getBookingConfirmation());
        if (dto.getBookingReminder() != null) pref.setBookingReminder(dto.getBookingReminder());
        if (dto.getCheckInReminder() != null) pref.setCheckInReminder(dto.getCheckInReminder());
        if (dto.getCheckOutReminder() != null) pref.setCheckOutReminder(dto.getCheckOutReminder());
        if (dto.getPaymentNotifications() != null) pref.setPaymentNotifications(dto.getPaymentNotifications());
        if (dto.getPromotionalOffers() != null) pref.setPromotionalOffers(dto.getPromotionalOffers());
        if (dto.getLoyaltyUpdates() != null) pref.setLoyaltyUpdates(dto.getLoyaltyUpdates());
        if (dto.getReviewRequests() != null) pref.setReviewRequests(dto.getReviewRequests());
        if (dto.getAccountAlerts() != null) pref.setAccountAlerts(dto.getAccountAlerts());
        if (dto.getQuietHoursEnabled() != null) pref.setQuietHoursEnabled(dto.getQuietHoursEnabled());
        if (dto.getQuietHoursStart() != null) pref.setQuietHoursStart(dto.getQuietHoursStart());
        if (dto.getQuietHoursEnd() != null) pref.setQuietHoursEnd(dto.getQuietHoursEnd());
        if (dto.getUnsubscribedTypes() != null) pref.setUnsubscribedTypes(dto.getUnsubscribedTypes());

        UserNotificationPreference saved = preferenceRepository.save(pref);
        return modelMapper.map(saved, UserNotificationPreferenceDTO.class);
    }
    
    /**
     * Get count of unread notifications
     */
    public Long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    /**
     * Process scheduled notifications
     * Runs every minute
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void processScheduledNotifications() {
        log.debug("Processing scheduled notifications...");
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Notification> scheduled = notificationRepository.findScheduledNotificationsToSend(now);
            
            for (Notification notification : scheduled) {
                try {
                    processNotification(notification, null);
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setSentTime(LocalDateTime.now());
                } catch (Exception e) {
                    log.error("Failed to process scheduled notification {}: {}", notification.getId(), e.getMessage());
                    notification.setStatus(NotificationStatus.FAILED);
                    notification.setErrorMessage(e.getMessage());
                    notification.setRetryCount(notification.getRetryCount() + 1);
                }
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            log.error("Error processing scheduled notifications: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Retry failed notifications
     * Runs every 5 minutes
     */
    @Scheduled(fixedRate = 300000) // Every 300 seconds (5 minutes)
    public void retryFailedNotifications() {
        log.debug("Retrying failed notifications...");
        try {
            Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 100);
            Page<Notification> failed = notificationRepository.findByStatusAndRetryCountLessThan(
                NotificationStatus.FAILED, 3, pageable);
            
            for (Notification notification : failed.getContent()) {
                try {
                    processNotification(notification, null);
                    notification.setStatus(NotificationStatus.SENT);
                    notification.setSentTime(LocalDateTime.now());
                } catch (Exception e) {
                    notification.setRetryCount(notification.getRetryCount() + 1);
                    notification.setErrorMessage(e.getMessage());
                    if (notification.getRetryCount() >= notification.getMaxRetries()) {
                        notification.setStatus(NotificationStatus.FAILED);
                    }
                }
                notificationRepository.save(notification);
            }
        } catch (Exception e) {
            log.error("Error retrying failed notifications: {}", e.getMessage(), e);
        }
    }
    
    // Helper methods
    
    private void processNotification(Notification notification, SendNotificationRequest request) {
        switch (notification.getChannel()) {
            case EMAIL:
                sendEmailNotification(notification, request);
                break;
            case SMS:
                sendSmsNotification(notification, request);
                break;
            case IN_APP:
                // IN_APP notifications are just stored in DB
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentTime(LocalDateTime.now());
                break;
            default:
                throw new NotificationException("Unknown notification channel: " + notification.getChannel());
        }
    }
    
    private void sendEmailNotification(Notification notification, SendNotificationRequest request) {
        if (notification.getRecipientEmail() == null || notification.getRecipientEmail().isEmpty()) {
            throw new NotificationException("Recipient email is required for email notifications");
        }
        
        String subject = notification.getSubject() != null ? notification.getSubject() : "Notification";
        String content = notification.getMessage();
        
        emailService.sendSimpleEmail(notification.getRecipientEmail(), subject, content);
        
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentTime(LocalDateTime.now());
        log.info("Email notification sent to: {}", notification.getRecipientEmail());
    }
    
    private void sendSmsNotification(Notification notification, SendNotificationRequest request) {
        if (notification.getRecipientPhone() == null || notification.getRecipientPhone().isEmpty()) {
            throw new NotificationException("Recipient phone is required for SMS notifications");
        }
        
        String message = notification.getMessage();
        if (message.length() > 160) {
            message = message.substring(0, 157) + "...";
        }
        
        smsService.sendSms(notification.getRecipientPhone(), message);
        
        notification.setStatus(NotificationStatus.SENT);
        notification.setSentTime(LocalDateTime.now());
        log.info("SMS notification sent to: {}", notification.getRecipientPhone());
    }
    
    private Notification createNotificationEntity(SendNotificationRequest request) {
        Notification notification = new Notification();
        notification.setUserId(request.getUserId());
        notification.setType(request.getType());
        notification.setChannel(request.getChannel());
        notification.setRecipientEmail(request.getRecipientEmail());
        notification.setRecipientPhone(request.getRecipientPhone());
        notification.setSubject(request.getSubject());
        notification.setMessage(request.getMessage());
        notification.setTemplateId(request.getTemplateId());
        notification.setBookingId(request.getBookingId());
        notification.setHotelId(request.getHotelId());
        notification.setScheduledTime(request.getScheduledTime());
        
        return notification;
    }
    
    private UserNotificationPreference getOrCreateUserPreference(Long userId) {
        return preferenceRepository.findByUserId(userId)
            .orElseGet(() -> {
                UserNotificationPreference pref = new UserNotificationPreference();
                pref.setUserId(userId);
                return preferenceRepository.save(pref);
            });
    }
    
    private boolean isNotificationAllowed(UserNotificationPreference preference, NotificationType type) {
        // Check if notification type is unsubscribed
        if (preference.getUnsubscribedTypes() != null && !preference.getUnsubscribedTypes().isEmpty()) {
            String[] unsubscribed = preference.getUnsubscribedTypes().split(",");
            for (String unsubType : unsubscribed) {
                if (unsubType.trim().equals(type.toString())) {
                    return false;
                }
            }
        }
        
        // Check specific notification type preferences
        return switch (type) {
            case BOOKING_CONFIRMATION -> preference.getBookingConfirmation();
            case BOOKING_REMINDER -> preference.getBookingReminder();
            case CHECK_IN_REMINDER -> preference.getCheckInReminder();
            case CHECK_OUT_REMINDER -> preference.getCheckOutReminder();
            case PAYMENT_RECEIVED, PAYMENT_FAILED -> preference.getPaymentNotifications();
            case PROMOTIONAL_OFFER -> preference.getPromotionalOffers();
            case LOYALTY_POINTS_EARNED -> preference.getLoyaltyUpdates();
            case REVIEW_REQUEST -> preference.getReviewRequests();
            case ACCOUNT_ALERT -> preference.getAccountAlerts();
            default -> true;
        };
    }
}
