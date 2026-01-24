package com.travolish.traveller.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * SMS Service for sending SMS notifications
 * Current implementation is a mock. For production, integrate with Twilio or AWS SNS
 */
@Service
@Slf4j
public class SmsService {
    
    @Value("${sms.enabled:false}")
    private boolean smsEnabled;
    
    @Value("${sms.provider:mock}")
    private String smsProvider; // mock, twilio, aws-sns
    
    /**
     * Send SMS message
     * For production, integrate with actual SMS provider
     */
    public void sendSms(String phoneNumber, String message) {
        try {
            if (!smsEnabled) {
                log.warn("SMS is disabled. Message not sent to: {}", phoneNumber);
                return;
            }
            
            // Validate phone number format
            if (!isValidPhoneNumber(phoneNumber)) {
                throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
            }
            
            // Mock implementation - log the SMS
            log.info("SMS message sent to {}: {}", phoneNumber, message);
            
            // TODO: Integrate with actual SMS provider (Twilio, AWS SNS, etc.)
            // Example for Twilio:
            // TwilioRestClient.sendSms(phoneNumber, message);
            
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send SMS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Send batch SMS messages
     */
    public void sendBatchSms(String[] phoneNumbers, String message) {
        try {
            for (String phoneNumber : phoneNumbers) {
                sendSms(phoneNumber, message);
            }
            log.info("Batch SMS sent to {} recipients", phoneNumbers.length);
        } catch (Exception e) {
            log.error("Failed to send batch SMS: {}", e.getMessage());
            throw new RuntimeException("Failed to send batch SMS: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate phone number format
     */
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Basic validation - accept formats like: +1234567890, 1234567890, +1 (234) 567-8900
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return false;
        }
        // Remove common separators
        String cleaned = phoneNumber.replaceAll("[\\s\\-().]", "");
        // Should start with + or digit and contain only digits
        return cleaned.matches("^\\+?\\d{10,15}$");
    }
    
    /**
     * Check if SMS service is enabled
     */
    public boolean isSmsEnabled() {
        return smsEnabled;
    }
}
