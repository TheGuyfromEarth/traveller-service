package com.travolish.traveller.notifications.entity;

public enum NotificationChannel {
    EMAIL("Email"),
    SMS("SMS"),
    IN_APP("In-App"),
    PUSH("Push Notification"),
    // TODO (WhatsApp — Phase 3): Add WHATSAPP channel.
    //   • WhatsApp messages are sent via Meta Cloud API (graph.facebook.com) — no Twilio involved.
    //   • NotificationService.sendNotification() must branch on this channel and call
    //     WhatsAppService instead of SmsService.
    //   • Add `whatsapp_enabled` column to user_notification_preferences (see UserNotificationPreference).
    //   Uncomment when WhatsAppService is implemented:
    // WHATSAPP("WhatsApp"),
    ;
    
    private final String displayName;
    
    NotificationChannel(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
