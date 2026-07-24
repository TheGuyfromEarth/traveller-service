package com.travolish.traveller.booking.service;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.hosttools.entity.AutoReplyTemplate;
import com.travolish.traveller.hosttools.repository.AutoReplyTemplateRepository;
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
import java.util.HashSet;
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
 *
 * If a host has an active AutoReplyTemplate for the matching trigger keyword, the
 * template text is used as the message body. If the host explicitly deactivated
 * the template, the reminder is skipped for their guests. If no template exists,
 * the hardcoded default body is sent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GuestReminderScheduler {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AutoReplyTemplateRepository autoReplyTemplateRepository;

    /** Pre-arrival + check-in-day reminders sent at 08:00 every morning. */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendMorningReminders() {
        LocalDate today = LocalDate.now();
        LocalDate dayAfterTomorrow = today.plusDays(2);

        List<Booking> preArrival = bookingRepository
            .findByStatusAndCheckInDate(Booking.BookingStatus.CONFIRMED, dayAfterTomorrow);
        sendBatch(preArrival, NotificationType.BOOKING_REMINDER, "Your check-in is in 2 days!", "pre-arrival", "pre-arrival");

        List<Booking> checkInToday = bookingRepository
            .findByStatusAndCheckInDate(Booking.BookingStatus.CONFIRMED, today);
        sendBatch(checkInToday, NotificationType.CHECK_IN_REMINDER, "Today is your check-in day!", "check-in day", "check-in");
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
            "Checkout is tomorrow — see you again soon!", "checkout", "checkout");
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void sendBatch(List<Booking> bookings, NotificationType type, String subject,
                           String kind, String triggerKeyword) {
        if (bookings.isEmpty()) {
            log.info("GuestReminderScheduler: no {} reminders to send", kind);
            return;
        }

        // Batch-fetch hotels for names and host IDs — avoids N+1
        Set<Long> hotelIds = bookings.stream().map(Booking::getHotelId).collect(Collectors.toSet());
        List<Hotel> hotels = hotelRepository.findAllById(hotelIds);
        Map<Long, String> hotelNames = hotels.stream()
            .collect(Collectors.toMap(Hotel::getId, Hotel::getName));
        Map<Long, Long> hotelToHostId = hotels.stream()
            .filter(h -> h.getHostId() != null)
            .collect(Collectors.toMap(Hotel::getId, Hotel::getHostId));

        // Batch-fetch host reminder templates (active or deactivated, not archived) — one query
        Set<Long> hostIds = new HashSet<>(hotelToHostId.values());
        Map<Long, AutoReplyTemplate> templatesByHost = hostIds.isEmpty() ? Map.of()
            : autoReplyTemplateRepository
                .findByHostIdInAndTriggerKeyword(hostIds, triggerKeyword).stream()
                .collect(Collectors.toMap(AutoReplyTemplate::getHostId, t -> t, (a, b) -> a));

        int sent = 0;
        for (Booking b : bookings) {
            try {
                Long hostId = hotelToHostId.get(b.getHotelId());
                AutoReplyTemplate tmpl = hostId != null ? templatesByHost.get(hostId) : null;

                // Host explicitly disabled this reminder — skip their guests
                if (tmpl != null && Boolean.FALSE.equals(tmpl.getIsActive())) {
                    log.debug("GuestReminderScheduler: {} reminder skipped for booking {} — host disabled",
                        kind, b.getId());
                    continue;
                }

                String hotelName = hotelNames.getOrDefault(b.getHotelId(), "your accommodation");
                String body = (tmpl != null)
                    ? tmpl.getTemplateText()
                    : switch (type) {
                        case BOOKING_REMINDER  -> buildPreArrivalBody(b, hotelName);
                        case CHECK_IN_REMINDER -> buildCheckInBody(b, hotelName);
                        case CHECK_OUT_REMINDER -> buildCheckOutBody(b, hotelName);
                        default -> b.getGuestName() + ", this is a reminder about your stay at " + hotelName + ".";
                    };

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
                req.setUserId(b.getUserId());
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
