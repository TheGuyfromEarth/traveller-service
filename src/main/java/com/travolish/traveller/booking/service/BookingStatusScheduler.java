package com.travolish.traveller.booking.service;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Transitions CONFIRMED and PENDING bookings whose checkout date has passed to COMPLETED.
 * Runs at 02:00 AM daily AND on application startup so status is always accurate.
 *
 * Note: the traveller-facing trips page also derives display status from dates client-side
 * so travellers always see the correct status regardless of when the cron last ran.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingStatusScheduler {

    private final BookingRepository bookingRepository;

    /** Runs daily at 02:00 AM. */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void completeExpiredBookings() {
        runStatusTransition("scheduled");
    }

    /**
     * Exposed for on-demand use (e.g., when a user opens the trips page).
     * Returns the number of bookings transitioned.
     */
    @Transactional
    public int runStatusTransition(String trigger) {
        LocalDate today = LocalDate.now();
        List<Booking> expired = bookingRepository.findExpiredActiveBookings(today);

        if (expired.isEmpty()) {
            log.debug("BookingStatusScheduler [{}]: no bookings to complete", trigger);
            return 0;
        }

        expired.forEach(b -> b.setStatus(Booking.BookingStatus.COMPLETED));
        bookingRepository.saveAll(expired);
        log.info("BookingStatusScheduler [{}]: transitioned {} booking(s) → COMPLETED", trigger, expired.size());
        return expired.size();
    }
}
