package com.travolish.traveller.notifications.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsService {

    @Value("${sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${sms.provider:mock}")
    private String smsProvider;

    @Value("${sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${sms.twilio.auth-token:}")
    private String authToken;

    @Value("${sms.twilio.phone-number:}")
    private String twilioPhoneNumber;

    @PostConstruct
    public void init() {
        if ("twilio".equalsIgnoreCase(smsProvider) && !accountSid.isBlank() && !authToken.isBlank()) {
            Twilio.init(accountSid, authToken);
            log.info("Twilio SMS service initialized (from={})", twilioPhoneNumber);
        } else {
            log.info("SMS running in mock mode (provider={}, enabled={})", smsProvider, smsEnabled);
        }
    }

    public void sendSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.warn("SMS disabled — skipping message to {}", phoneNumber);
            return;
        }
        if (!isValidPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
        }

        try {
            if ("twilio".equalsIgnoreCase(smsProvider)) {
                Message.creator(
                    new PhoneNumber(phoneNumber),
                    new PhoneNumber(twilioPhoneNumber),
                    message
                ).create();
                log.info("SMS sent via Twilio to {}", phoneNumber);
            } else {
                log.info("Mock SMS to {}: {}", phoneNumber, message);
            }
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage());
            throw new RuntimeException("Failed to send SMS: " + e.getMessage(), e);
        }
    }

    public void sendBatchSms(String[] phoneNumbers, String message) {
        for (String phoneNumber : phoneNumbers) {
            sendSms(phoneNumber, message);
        }
        log.info("Batch SMS sent to {} recipients", phoneNumbers.length);
    }

    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) return false;
        String cleaned = phoneNumber.replaceAll("[\\s\\-().]", "");
        return cleaned.matches("^\\+?\\d{10,15}$");
    }
}
