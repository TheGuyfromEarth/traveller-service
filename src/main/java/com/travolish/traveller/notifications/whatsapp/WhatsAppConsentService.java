package com.travolish.traveller.notifications.whatsapp;

import com.travolish.traveller.notifications.entity.UserNotificationPreference;
import com.travolish.traveller.notifications.repository.UserNotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WhatsAppConsentService {

    private final WhatsAppOptInRepository optInRepository;
    private final UserNotificationPreferenceRepository preferenceRepository;

    @Transactional
    public void recordOptIn(Long userId, String phoneE164, WhatsAppOptInSource source, String ipAddress, String userAgent) {
        optInRepository.findActiveByUserId(userId).ifPresent(existing -> {
            existing.setRevokedAt(LocalDateTime.now());
            optInRepository.save(existing);
        });

        WhatsAppOptIn row = WhatsAppOptIn.builder()
                .userId(userId)
                .phoneE164(normalizeE164(phoneE164))
                .optedInAt(LocalDateTime.now())
                .source(source)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        optInRepository.save(row);

        UserNotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserNotificationPreference p = new UserNotificationPreference();
                    p.setUserId(userId);
                    return preferenceRepository.save(p);
                });
        pref.setWhatsappEnabled(true);
        pref.setWhatsappPhoneVerified(false);
        preferenceRepository.save(pref);
    }

    @Transactional
    public void revoke(Long userId) {
        optInRepository.findActiveByUserId(userId).ifPresent(existing -> {
            existing.setRevokedAt(LocalDateTime.now());
            optInRepository.save(existing);
        });
        preferenceRepository.findByUserId(userId).ifPresent(pref -> {
            pref.setWhatsappEnabled(false);
            preferenceRepository.save(pref);
        });
    }

    @Transactional(readOnly = true)
    public boolean isActive(Long userId) {
        return optInRepository.findActiveByUserId(userId).isPresent();
    }

    public static String normalizeE164(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone is required");
        }
        String cleaned = phone.trim().replaceAll("[\\s\\-().]", "");
        if (!cleaned.startsWith("+")) {
            throw new IllegalArgumentException("Phone must be E.164 format starting with +");
        }
        if (!cleaned.matches("^\\+[1-9]\\d{7,14}$")) {
            throw new IllegalArgumentException("Invalid E.164 phone number");
        }
        return cleaned;
    }
}
