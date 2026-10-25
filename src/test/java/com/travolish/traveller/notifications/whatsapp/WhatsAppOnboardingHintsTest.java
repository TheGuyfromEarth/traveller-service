package com.travolish.traveller.notifications.whatsapp;

import com.travolish.traveller.notifications.entity.UserNotificationPreference;
import com.travolish.traveller.notifications.repository.UserNotificationPreferenceRepository;
import com.travolish.traveller.user.entity.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WhatsAppOnboardingHintsTest {

    @Test
    void requiresOnboarding_whenNoPhone() {
        WhatsAppConsentService consent = mock(WhatsAppConsentService.class);
        UserNotificationPreferenceRepository prefs = mock(UserNotificationPreferenceRepository.class);
        WhatsAppOnboardingHints hints = new WhatsAppOnboardingHints(consent, prefs);

        User user = User.builder().id(1L).email("a@b.com").build();
        assertTrue(hints.isOnboardingRequired(user));
    }

    @Test
    void complete_whenOptInAndVerified() {
        WhatsAppConsentService consent = mock(WhatsAppConsentService.class);
        UserNotificationPreferenceRepository prefs = mock(UserNotificationPreferenceRepository.class);
        WhatsAppOnboardingHints hints = new WhatsAppOnboardingHints(consent, prefs);

        User user = User.builder().id(1L).phone("+919876543210").build();
        when(consent.isActive(1L)).thenReturn(true);
        UserNotificationPreference pref = new UserNotificationPreference();
        pref.setWhatsappPhoneVerified(true);
        when(prefs.findByUserId(1L)).thenReturn(Optional.of(pref));

        assertFalse(hints.isOnboardingRequired(user));
    }
}
