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
import java.util.Optional;

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

        // Pre-arrival: check-in exactly 48 h from now
        List<Booking> preArrival = bookingRepository.findAll().stream()
            .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED
                      && dayAfterTomorrow.equals(b.getCheckInDate()))
            .toList();
        preArrival.forEach(b -> sendReminder(b, NotificationType.BOOKING_REMINDER,
            "Your check-in is in 2 days!",
            buildPreArrivalBody(b)));
        log.info("GuestReminderScheduler: sent {} pre-arrival reminder(s)", preArrival.size());

        // Check-in day note
        List<Booking> checkInToday = bookingRepository.findAll().stream()
            .filter(b -> b.getStatus() == Booking.BookingStatus.CONFIRMED
                      && today.equals(b.getCheckInDate()))
            .toList();
        checkInToday.forEach(b -> sendReminder(b, NotificationType.CHECK_IN_REMINDER,
            "Today is your check-in day!",
            buildCheckInBody(b)));
        log.info("GuestReminderScheduler: sent {} check-in day reminder(s)", checkInToday.size());
    }

    /** Checkout reminders sent at 18:00 the evening before checkout. */
    @Scheduled(cron = "0 0 18 * * *")
    public void sendEveningCheckoutReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);

        List<Booking> checkOutTomorrow = bookingRepository.findAll().stream()
            .filter(b -> (b.getStatus() == Booking.BookingStatus.CONFIRMED
                       || b.getStatus() == Booking.BookingStatus.COMPLETED)
                      && tomorrow.equals(b.getCheckOutDate()))
            .toList();
        checkOutTomorrow.forEach(b -> sendReminder(b, NotificationType.CHECK_OUT_REMINDER,
            "Checkout is tomorrow — see you again soon!",
            buildCheckOutBody(b)));
        log.info("GuestReminderScheduler: sent {} checkout reminder(s)", checkOutTomorrow.size());
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void sendReminder(Booking booking, NotificationType type, String subject, String body) {
        try {
            Optional<User> userOpt = userRepository.findById(booking.getId());
            String recipientEmail = userOpt.map(User::getEmail).orElse(booking.getGuestEmail());
            if (recipientEmail == null || recipientEmail.isBlank()) return;

            SendNotificationRequest req = new SendNotificationRequest();
            req.setUserId(booking.getId());
            req.setType(type);
            req.setChannel(NotificationChannel.EMAIL);
            req.setRecipientEmail(recipientEmail);
            req.setSubject(subject);
            req.setMessage(body);
            req.setSendImmediately(true);
            notificationService.sendNotificationAsync(req);
        } catch (Exception e) {
            log.warn("GuestReminderScheduler: could not send {} for booking {}: {}",
                type, booking.getId(), e.getMessage());
        }
    }

    private String buildPreArrivalBody(Booking b) {
        String hotel = hotelRepository.findById(b.getHotelId())
            .map(Hotel::getName).orElse("your accommodation");
        return String.format("""
            Hi %s,

            Your check-in at %s is coming up in 2 days (on %s).

            Make sure you have the host's contact details and directions ready.
            View your trip details in the Travolish app.

            Safe travels!
            — The Travolish Team
            """, b.getGuestName(), hotel, b.getCheckInDate());
    }

    private String buildCheckInBody(Booking b) {
        String hotel = hotelRepository.findById(b.getHotelId())
            .map(Hotel::getName).orElse("your accommodation");
        return String.format("""
            Hi %s,

            Today is your check-in day at %s! 🎉

            If you have any issues on arrival, contact your host directly through the Travolish app.

            Enjoy your stay!
            — The Travolish Team
            """, b.getGuestName(), hotel);
    }

    private String buildCheckOutBody(Booking b) {
        String hotel = hotelRepository.findById(b.getHotelId())
            .map(Hotel::getName).orElse("your accommodation");
        return String.format("""
            Hi %s,

            Friendly reminder: your checkout from %s is tomorrow (%s).

            Please leave by the agreed checkout time and return any keys or access cards.
            We hope you had a wonderful stay — please leave a review in the Travolish app!

            — The Travolish Team
            """, b.getGuestName(), hotel, b.getCheckOutDate());
    }
}
