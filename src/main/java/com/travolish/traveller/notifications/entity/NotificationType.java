package com.travolish.traveller.notifications.entity;

public enum NotificationType {
    BOOKING_CONFIRMATION("Booking Confirmed"),
    BOOKING_REMINDER("Booking Reminder"),
    BOOKING_CANCELLATION("Booking Cancelled"),
    BOOKING_MODIFIED("Booking Modified"),
    CHECK_IN_REMINDER("Check-in Reminder"),
    CHECK_OUT_REMINDER("Check-out Reminder"),
    PAYMENT_RECEIVED("Payment Received"),
    PAYMENT_FAILED("Payment Failed"),
    SPECIAL_REQUEST_CONFIRMATION("Special Request Confirmed"),
    REVIEW_REQUEST("Please Leave a Review"),
    PROMOTIONAL_OFFER("Special Offer"),
    LOYALTY_POINTS_EARNED("Loyalty Points Earned"),
    WELCOME("Welcome to Travolish"),
    PASSWORD_RESET("Password Reset Request"),
    EMAIL_VERIFICATION("Email Verification"),
    ACCOUNT_ALERT("Account Alert"),
    MAINTENANCE_NOTICE("Maintenance Notice"),
    GENERAL_ANNOUNCEMENT("General Announcement");
    
    private final String description;
    
    NotificationType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
