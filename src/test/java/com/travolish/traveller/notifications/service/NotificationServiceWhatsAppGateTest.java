package com.travolish.traveller.notifications.service;

import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import com.travolish.traveller.notifications.entity.UserNotificationPreference;
import com.travolish.traveller.notifications.repository.NotificationRepository;
import com.travolish.traveller.notifications.repository.NotificationTemplateRepository;
import com.travolish.traveller.notifications.repository.UserNotificationPreferenceRepository;
import com.travolish.traveller.notifications.whatsapp.WhatsAppConsentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceWhatsAppGateTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationTemplateRepository templateRepository;
    @Mock private UserNotificationPreferenceRepository preferenceRepository;
    @Mock private EmailService emailService;
    @Mock private SmsService smsService;
    @Mock private WhatsAppService whatsAppService;
    @Mock private WhatsAppConsentService whatsAppConsentService;
    @Mock private ApplicationContext applicationContext;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository,
                templateRepository,
                preferenceRepository,
                emailService,
                smsService,
                whatsAppService,
                whatsAppConsentService,
                new ModelMapper(),
                applicationContext);
    }

    @Test
    void blocksWhatsApp_whenPhoneNotVerified() {
        Long userId = 42L;
        UserNotificationPreference pref = new UserNotificationPreference();
        pref.setUserId(userId);
        pref.setWhatsappEnabled(true);
        pref.setWhatsappPhoneVerified(false);
        pref.setBookingConfirmation(true);
        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(pref));
        when(notificationRepository.existsSuccessfulWhatsAppForUser(any(), any(), any())).thenReturn(false);

        SendNotificationRequest req = new SendNotificationRequest();
        req.setUserId(userId);
        req.setType(NotificationType.WELCOME);
        req.setChannel(NotificationChannel.WHATSAPP);
        req.setRecipientPhone("+919876543210");
        req.setSendImmediately(true);

        assertNull(notificationService.sendNotification(req));
        verify(whatsAppService, never()).sendTemplateForNotificationType(any(), any(), any());
    }
}
