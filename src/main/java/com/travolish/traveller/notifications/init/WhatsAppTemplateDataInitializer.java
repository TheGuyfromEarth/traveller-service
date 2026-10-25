package com.travolish.traveller.notifications.init;

import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationTemplate;
import com.travolish.traveller.notifications.entity.NotificationType;
import com.travolish.traveller.notifications.repository.NotificationTemplateRepository;
import com.travolish.traveller.notifications.whatsapp.WhatsAppTemplateCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WhatsAppTemplateDataInitializer {

    private final NotificationTemplateRepository templateRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void seedWhatsAppTemplates() {
        WhatsAppTemplateCatalog.forType(NotificationType.WELCOME).ifPresent(def -> upsert(NotificationType.WELCOME, def.templateName()));
        WhatsAppTemplateCatalog.forType(NotificationType.BOOKING_CONFIRMATION).ifPresent(def -> upsert(NotificationType.BOOKING_CONFIRMATION, def.templateName()));
        WhatsAppTemplateCatalog.forType(NotificationType.BOOKING_REMINDER).ifPresent(def -> upsert(NotificationType.BOOKING_REMINDER, def.templateName()));
        WhatsAppTemplateCatalog.forType(NotificationType.CHECK_IN_REMINDER).ifPresent(def -> upsert(NotificationType.CHECK_IN_REMINDER, def.templateName()));
        WhatsAppTemplateCatalog.forType(NotificationType.CHECK_OUT_REMINDER).ifPresent(def -> upsert(NotificationType.CHECK_OUT_REMINDER, def.templateName()));
        WhatsAppTemplateCatalog.forType(NotificationType.BOOKING_CANCELLATION).ifPresent(def -> upsert(NotificationType.BOOKING_CANCELLATION, def.templateName()));
        WhatsAppTemplateCatalog.forType(NotificationType.PROMOTIONAL_OFFER).ifPresent(def -> upsert(NotificationType.PROMOTIONAL_OFFER, def.templateName()));
    }

    private void upsert(NotificationType type, String metaTemplateName) {
        templateRepository.findByTypeAndChannel(type, NotificationChannel.WHATSAPP).ifPresentOrElse(
                existing -> {
                    if (!metaTemplateName.equals(existing.getName())) {
                        existing.setName(metaTemplateName);
                        templateRepository.save(existing);
                    }
                },
                () -> {
                    NotificationTemplate row = new NotificationTemplate();
                    row.setType(type);
                    row.setChannel(NotificationChannel.WHATSAPP);
                    row.setName(metaTemplateName);
                    row.setDescription("Meta WhatsApp template: " + metaTemplateName);
                    row.setMessageTemplate(metaTemplateName);
                    row.setIsActive(true);
                    templateRepository.save(row);
                    log.info("Seeded WhatsApp notification template {} -> {}", type, metaTemplateName);
                });
    }
}
