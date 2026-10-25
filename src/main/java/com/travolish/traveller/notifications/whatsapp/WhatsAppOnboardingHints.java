package com.travolish.traveller.notifications.whatsapp;

import com.travolish.traveller.notifications.repository.UserNotificationPreferenceRepository;
import com.travolish.traveller.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WhatsAppOnboardingHints {

    private final WhatsAppConsentService consentService;
    private final UserNotificationPreferenceRepository preferenceRepository;

    /**
     * True when the client should prompt for phone, WhatsApp opt-in, and/or OTP verification.
     */
    public boolean isOnboardingRequired(User user) {
        if (user == null || user.getId() == null) {
            return true;
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            return true;
        }
        if (!consentService.isActive(user.getId())) {
            return true;
        }
        return preferenceRepository.findByUserId(user.getId())
                .map(p -> !Boolean.TRUE.equals(p.getWhatsappPhoneVerified()))
                .orElse(true);
    }
}
