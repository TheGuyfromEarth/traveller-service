package com.travolish.traveller.notifications.exception;

public class NotificationNotFoundException extends NotificationException {
    public NotificationNotFoundException(Long id) {
        super("Notification not found with id: " + id);
    }
    
    public NotificationNotFoundException(String message) {
        super(message);
    }
}
