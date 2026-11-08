package com.travolish.traveller.notifications.whatsapp;

import com.travolish.traveller.notifications.entity.NotificationType;

import java.util.Map;
import java.util.Optional;

/**
 * Maps notification types to Meta-approved template names (configure in Business Manager).
 */
public final class WhatsAppTemplateCatalog {

    private WhatsAppTemplateCatalog() {}

    public record TemplateDef(String templateName, String languageCode, MetaTemplateCategory category) {}

    private static final Map<NotificationType, TemplateDef> BY_TYPE = Map.ofEntries(
            Map.entry(NotificationType.WELCOME, new TemplateDef("welcome_travolish", "en", MetaTemplateCategory.UTILITY)),
            Map.entry(NotificationType.BOOKING_CONFIRMATION, new TemplateDef("booking_confirmation", "en", MetaTemplateCategory.UTILITY)),
            Map.entry(NotificationType.BOOKING_REMINDER, new TemplateDef("booking_reminder", "en", MetaTemplateCategory.UTILITY)),
            Map.entry(NotificationType.CHECK_IN_REMINDER, new TemplateDef("check_in_reminder", "en", MetaTemplateCategory.UTILITY)),
            Map.entry(NotificationType.CHECK_OUT_REMINDER, new TemplateDef("check_out_reminder", "en", MetaTemplateCategory.UTILITY)),
            Map.entry(NotificationType.BOOKING_CANCELLATION, new TemplateDef("booking_cancellation", "en", MetaTemplateCategory.UTILITY)),
            Map.entry(NotificationType.PROMOTIONAL_OFFER, new TemplateDef("promotional_offer", "en", MetaTemplateCategory.MARKETING))
    );

    public static Optional<TemplateDef> forType(NotificationType type) {
        return Optional.ofNullable(BY_TYPE.get(type));
    }
}
