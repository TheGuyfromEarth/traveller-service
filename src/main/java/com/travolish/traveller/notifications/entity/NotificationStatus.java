package com.travolish.traveller.notifications.entity;

public enum NotificationStatus {
    PENDING("Pending"),
    SCHEDULED("Scheduled"),
    SENT("Sent"),
    DELIVERED("Delivered"),
    FAILED("Failed"),
    RETRY("Retry");
    
    private final String displayName;
    
    NotificationStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
