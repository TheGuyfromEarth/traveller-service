package com.travolish.traveller.booking.service;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.notifications.dto.SendNotificationRequest;
import com.travolish.traveller.notifications.entity.NotificationChannel;
import com.travolish.traveller.notifications.entity.NotificationType;
import com.travolish.traveller.notifications.service.NotificationService;
import com.travolish.traveller.user.entity.User;
import com.travolish.traveller.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sends timed guest reminders by creating Notification records that
 * NotificationService.processScheduledNotifications() then dispatches via email.
 *
 *  Pre-arrival  : 48 h before check-in  (daily at 08:00)
 *  Check-in day : morning of check-in   (daily at 08:00)
 *  Checkout     : evening before        (daily at 18:00)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GuestReminderScheduler {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /** Pre-arrival + check-in-day reminders sent at 08:00 every morning. */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendMorningReminders() {
        LocalDate today = LocalDate.now();
        LocalDate dayAfterTomorrow = today.plusDays(2);

        // Pre-arrival: check-in exactly 48 h from now — DB-filtered, no findAll()
        List<Booking> preArrival = bookingRepository
            .findByStatusAndCheckInDate(Booking.BookingStatus.CONFIRMED, dayAfterTomorrow);
        sendBatch(preArrival, NotificationType.BOOKING_REMINDER, "Your check-in is in 2 days!", "pre-arrival");

        // Check-in day note
        List<Booking> checkInToday = bookingRepository
            .findByStatusAndCheckInDate(Booking.BookingStatus.CONFIRMED, today);
        sendBatch(checkInToday, NotificationType.CHECK_IN_REMINDER, "Today is your check-in day!", "check-in day");
    }

    /** Checkout reminders sent at 18:00 the evening before checkout. */
    @Scheduled(cron = "0 0 18 * * *")
    public void sendEveningCheckoutReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Booking> checkOutTomorrow = bookingRepository
            .findByStatusInAndCheckOutDate(
                List.of(Booking.BookingStatus.CONFIRMED, Booking.BookingStatus.COMPLETED),
                tomorrow);
        sendBatch(checkOutTomorrow, NotificationType.CHECK_OUT_REMINDER,
            "Checkout is tomorrow — see you again soon!", "checkout");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    /** Batch-send reminders for a list of bookings. Pre-fetches hotel names to avoid N+1. */
    private void sendBatch(List<Booking> bookings, NotificationType type, String subject, String kind) {
        if (bookings.isEmpty()) {
            log.info("GuestReminderScheduler: no {} reminders to send", kind);
            return;
        }

        // Batch-fetch hotel names — one query for all hotel IDs in this batch
        Set<Long> hotelIds = bookings.stream().map(Booking::getHotelId).collect(Collectors.toSet());
        Map<Long, String> hotelNames = hotelRepository.findAllById(hotelIds).stream()
            .collect(Collectors.toMap(Hotel::getId, Hotel::getName));

        int sent = 0;
        for (Booking b : bookings) {
            try {
                String hotelName = hotelNames.getOrDefault(b.getHotelId(), "your accommodation");
                String body = switch (type) {
                    case BOOKING_REMINDER -> buildPreArrivalBody(b, hotelName);
                    case CHECK_IN_REMINDER -> buildCheckInBody(b, hotelName);
                    case CHECK_OUT_REMINDER -> buildCheckOutBody(b, hotelName);
                    default -> b.getGuestName() + ", this is a reminder about your stay at " + hotelName + ".";
                };

                // Fix: look up by userId, not booking id; fall back to guestEmail for anonymous bookings
                String recipientEmail;
                if (b.getUserId() != null) {
                    recipientEmail = userRepository.findById(b.getUserId())
                        .map(User::getEmail)
                        .orElse(b.getGuestEmail());
                } else {
                    recipientEmail = b.getGuestEmail();
                }
                if (recipientEmail == null || recipientEmail.isBlank()) continue;

                SendNotificationRequest req = new SendNotificationRequest();
                req.setUserId(b.getUserId());      // correctly uses userId, not booking id
                req.setType(type);
                req.setChannel(NotificationChannel.EMAIL);
                req.setRecipientEmail(recipientEmail);
                req.setSubject(subject);
                req.setMessage(body);
                req.setSendImmediately(true);
                notificationService.sendNotificationAsync(req);
                sent++;
            } catch (Exception e) {
                log.warn("GuestReminderScheduler: could not send {} reminder for booking {}: {}",
                    kind, b.getId(), e.getMessage());
            }
        }
        log.info("GuestReminderScheduler: sent {} {} reminder(s)", sent, kind);
    }

    private String buildPreArrivalBody(Booking b, String hotel) {
        return String.format("""
            Hi %s,

            Your check-in at %s is coming up in 2 days (on %s).

            Make sure you have the host's contact details and directions ready.
            View your trip details in the Travolish app.

            Safe travels!
            — The Travolish Team
            """, b.getGuestName(), hotel, b.getCheckInDate());
    }

    private String buildCheckInBody(Booking b, String hotel) {
        return String.format("""
            Hi %s,

            Today is your check-in day at %s!

            If you have any issues on arrival, contact your host directly through the Travolish app.

            Enjoy your stay!
            — The Travolish Team
            """, b.getGuestName(), hotel);
    }

    private String buildCheckOutBody(Booking b, String hotel) {
        return String.format("""
            Hi %s,

            Friendly reminder: your checkout from %s is tomorrow (%s).

            Please leave by the agreed checkout time and return any keys or access cards.
            We hope you had a wonderful stay — please leave a review in the Travolish app!

            — The Travolish Team
            """, b.getGuestName(), hotel, b.getCheckOutDate());
    }
}
