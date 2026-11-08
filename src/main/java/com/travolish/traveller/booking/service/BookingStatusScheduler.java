package com.travolish.traveller.booking.service;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.inventory.service.AvailabilityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Completes past CONFIRMED stays and expires unpaid PENDING holds (releases inventory).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingStatusScheduler {

    private final BookingRepository bookingRepository;
    private final AvailabilityService availabilityService;

    @Value("${travolish.booking.pending-hold-minutes:30}")
    private int pendingHoldMinutes;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void completeExpiredBookings() {
        runStatusTransition("scheduled");
    }

    @Transactional
    public int runStatusTransition(String trigger) {
        int completed = completePastConfirmed(trigger);
        int expired = expireUnpaidPendingHolds(trigger);
        return completed + expired;
    }

    private int completePastConfirmed(String trigger) {
        LocalDate today = LocalDate.now();
        List<Booking> expired = bookingRepository.findExpiredConfirmedBookings(today);
        if (expired.isEmpty()) {
            return 0;
        }
        expired.forEach(b -> b.setStatus(Booking.BookingStatus.COMPLETED));
        bookingRepository.saveAll(expired);
        log.info("BookingStatusScheduler [{}]: transitioned {} booking(s) → COMPLETED", trigger, expired.size());
        return expired.size();
    }

    private int expireUnpaidPendingHolds(String trigger) {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(pendingHoldMinutes);
        List<Booking> holds = bookingRepository.findExpiredPendingHolds(cutoff);
        if (holds.isEmpty()) {
            return 0;
        }
        for (Booking booking : holds) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            booking.setUpdatedAt(OffsetDateTime.now());
            try {
                availabilityService.cancelBooking(
                        booking.getRoomId(), booking.getCheckInDate(), booking.getCheckOutDate());
            } catch (Exception e) {
                log.warn("Failed to release inventory for expired hold {}: {}", booking.getId(), e.getMessage());
            }
        }
        bookingRepository.saveAll(holds);
        log.info("BookingStatusScheduler [{}]: expired {} unpaid PENDING hold(s)", trigger, holds.size());
        return holds.size();
    }
}
