package com.travolish.traveller.notifications.service;

import com.travolish.traveller.auth.entity.PhoneVerificationCode;
import com.travolish.traveller.auth.repository.PhoneVerificationCodeRepository;
import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import com.travolish.traveller.notifications.entity.UserNotificationPreference;
import com.travolish.traveller.notifications.repository.UserNotificationPreferenceRepository;
import com.travolish.traveller.notifications.whatsapp.WhatsAppConsentService;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PhoneVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 6;
    private static final long CODE_TTL_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    private final PhoneVerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final UserNotificationPreferenceRepository preferenceRepository;
    private final NotificationService notificationService;
    private final WhatsAppConsentService consentService;

    @Transactional
    public void sendOtp(Long userId, String phoneE164) {
        if (!consentService.isActive(userId)) {
            throw new IllegalStateException("WhatsApp opt-in is required before phone verification");
        }
        String normalized = WhatsAppConsentService.normalizeE164(phoneE164);
        String code = generateCode();

        PhoneVerificationCode entity = PhoneVerificationCode.builder()
                .userId(userId)
                .phoneE164(normalized)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(CODE_TTL_MINUTES))
                .consumed(false)
                .attempts(0)
                .build();
        codeRepository.save(entity);

        smsService.sendSms(normalized, "Your Travolish verification code is: " + code);
        log.info("Phone OTP issued for user {}", userId);
    }

    @Transactional
    public void verifyOtp(Long userId, String phoneE164, String code) {
        String normalized = WhatsAppConsentService.normalizeE164(phoneE164);
        PhoneVerificationCode record = codeRepository.findLatestActiveByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No active verification code. Request a new code."));

        if (!record.getPhoneE164().equals(normalized)) {
            throw new IllegalArgumentException("Phone number does not match the active verification request.");
        }
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            record.setConsumed(true);
            codeRepository.save(record);
            throw new IllegalArgumentException("Verification code has expired.");
        }
        if (record.getAttempts() >= MAX_ATTEMPTS) {
            record.setConsumed(true);
            codeRepository.save(record);
            throw new IllegalArgumentException("Too many incorrect attempts.");
        }
        if (!record.getCode().equals(code.trim())) {
            record.setAttempts(record.getAttempts() + 1);
            codeRepository.save(record);
            throw new IllegalArgumentException("Incorrect verification code.");
        }

        record.setConsumed(true);
        codeRepository.save(record);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setPhone(normalized);
        userRepository.save(user);

        UserNotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserNotificationPreference p = new UserNotificationPreference();
                    p.setUserId(userId);
                    return preferenceRepository.save(p);
                });
        pref.setWhatsappPhoneVerified(true);
        preferenceRepository.save(pref);

        sendWelcomeWhatsApp(userId, normalized);
    }

    private void sendWelcomeWhatsApp(Long userId, String phoneE164) {
        SendNotificationRequest req = new SendNotificationRequest();
        req.setUserId(userId);
        req.setType(NotificationType.WELCOME);
        req.setChannel(NotificationChannel.WHATSAPP);
        req.setRecipientPhone(phoneE164);
        req.setSendImmediately(true);
        req.setTemplateVariables(Map.of("1", "Travolish"));
        notificationService.sendNotificationAsync(req);
    }

    private static String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        int code = RANDOM.nextInt(bound);
        return String.format("%0" + CODE_LENGTH + "d", code);
    }
}
