package com.travolish.traveller.notifications.service;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class GuestWhatsAppNotifier {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public void sendIfEligible(Booking booking, NotificationType type, Map<String, String> templateVariables) {
        if (booking.getUserId() == null) {
            return;
        }
        userRepository.findById(booking.getUserId()).ifPresent(user -> {
            if (user.getPhone() == null || user.getPhone().isBlank()) {
                return;
            }
            SendNotificationRequest wa = new SendNotificationRequest();
            wa.setUserId(user.getId());
            wa.setType(type);
            wa.setChannel(NotificationChannel.WHATSAPP);
            wa.setRecipientPhone(user.getPhone());
            wa.setBookingId(booking.getId());
            wa.setHotelId(booking.getHotelId());
            wa.setSendImmediately(true);
            wa.setTemplateVariables(templateVariables);
            notificationService.sendNotificationAsync(wa);
        });
    }

    public static Map<String, String> bookingConfirmationVars(Booking booking) {
        return Map.of(
                "1", booking.getGuestName() != null ? booking.getGuestName() : "Guest",
                "2", booking.getCheckInDate().toString(),
                "3", booking.getCheckOutDate().toString());
    }

    public static Map<String, String> bookingCancellationVars(Booking booking) {
        return Map.of(
                "1", booking.getGuestName() != null ? booking.getGuestName() : "Guest",
                "2", booking.getCheckInDate().toString(),
                "3", booking.getCheckOutDate().toString());
    }

    public static Map<String, String> reminderVars(Booking booking, String hotelName) {
        return Map.of(
                "1", booking.getGuestName() != null ? booking.getGuestName() : "Guest",
                "2", hotelName != null ? hotelName : "your stay",
                "3", booking.getCheckInDate().toString());
    }

    public static Map<String, String> checkOutReminderVars(Booking booking, String hotelName) {
        return Map.of(
                "1", booking.getGuestName() != null ? booking.getGuestName() : "Guest",
                "2", hotelName != null ? hotelName : "your stay",
                "3", booking.getCheckOutDate().toString());
    }
}
