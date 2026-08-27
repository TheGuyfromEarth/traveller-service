package com.travolish.traveller.notifications.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_notification_preferences", indexes = {
    @Index(name = "idx_preference_user", columnList = "user_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserNotificationPreference {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;
    
    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;
    
    @Column(name = "sms_enabled", nullable = false)
    private Boolean smsEnabled = false;

    // TODO (WhatsApp — Phase 3): Add WhatsApp preference columns.
    //   Run a DB migration (Flyway/Liquibase or manual ALTER TABLE) to add:
    //     ALTER TABLE user_notification_preferences
    //       ADD COLUMN whatsapp_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    //       ADD COLUMN whatsapp_phone_verified BOOLEAN NOT NULL DEFAULT FALSE;
    //   • Default false — users must explicitly opt in (Meta policy + GDPR requirement).
    //   • whatsapp_phone_verified tracks whether the phone number has been OTP-confirmed via
    //     Meta's own phone verification flow (see PhoneVerificationService, Phase 5).
    //     Only send WhatsApp messages when both columns are true.
    //   Uncomment when migration is applied:
    // @Column(name = "whatsapp_enabled", nullable = false)
    // private Boolean whatsappEnabled = false;
    //
    // @Column(name = "whatsapp_phone_verified", nullable = false)
    // private Boolean whatsappPhoneVerified = false;
    
    @Column(name = "in_app_enabled", nullable = false)
    private Boolean inAppEnabled = true;
    
    @Column(name = "booking_confirmation", nullable = false)
    private Boolean bookingConfirmation = true;
    
    @Column(name = "booking_reminder", nullable = false)
    private Boolean bookingReminder = true;
    
    @Column(name = "check_in_reminder", nullable = false)
    private Boolean checkInReminder = true;
    
    @Column(name = "check_out_reminder", nullable = false)
    private Boolean checkOutReminder = true;
    
    @Column(name = "payment_notifications", nullable = false)
    private Boolean paymentNotifications = true;
    
    @Column(name = "promotional_offers", nullable = false)
    private Boolean promotionalOffers = true;
    
    @Column(name = "loyalty_updates", nullable = false)
    private Boolean loyaltyUpdates = true;
    
    @Column(name = "review_requests", nullable = false)
    private Boolean reviewRequests = true;
    
    @Column(name = "account_alerts", nullable = false)
    private Boolean accountAlerts = true;
    
    @Column(name = "quiet_hours_enabled", nullable = false)
    private Boolean quietHoursEnabled = false;
    
    @Column(name = "quiet_hours_start", length = 5)
    private String quietHoursStart; // Format: HH:mm
    
    @Column(name = "quiet_hours_end", length = 5)
    private String quietHoursEnd; // Format: HH:mm
    
    @Column(name = "unsubscribed_types", length = 500)
    private String unsubscribedTypes; // Comma-separated list of NotificationType
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
