package com.travolish.traveller.notifications.entity;

public enum NotificationChannel {
    EMAIL("Email"),
    SMS("SMS"),
    IN_APP("In-App"),
    PUSH("Push Notification");
    
    private final String displayName;
    
    NotificationChannel(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
