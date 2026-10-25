package com.travolish.traveller.payout.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travolish.traveller.analytics.entity.HostEarnings;
import com.travolish.traveller.analytics.repository.HostEarningsRepository;
import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.config.PlatformEconomicsProperties;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.payment.entity.Payment;
import com.travolish.traveller.payout.HostEarningStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostLedgerService {

    private final HostEarningsRepository hostEarningsRepository;
    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;
    private final PlatformEconomicsProperties economicsProperties;

    @Transactional
    public void recordPaymentCaptured(Payment payment) {
        if (payment.getBookingId() == null || payment.getId() == null) {
            return;
        }
        Optional<HostEarnings> existing = hostEarningsRepository.findByPaymentId(payment.getId());
        if (existing.isPresent()) {
            return;
        }

        Booking booking = bookingRepository.findById(payment.getBookingId())
            .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + payment.getBookingId()));
        Hotel hotel = hotelRepository.findById(booking.getHotelId())
            .orElseThrow(() -> new IllegalArgumentException("Hotel not found: " + booking.getHotelId()));

        BigDecimal base = payment.getBaseAmount() != null ? payment.getBaseAmount() : payment.getAmount();
        BigDecimal net = payment.getNetAmount() != null ? payment.getNetAmount() : base;
        int nights = (int) Math.max(1, ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate()));

        HostEarnings row = new HostEarnings();
        row.setHostId(hotel.getHostId());
        row.setBookingId(booking.getId());
        row.setHotelId(booking.getHotelId());
        row.setRoomId(booking.getRoomId());
        row.setGuestId(booking.getUserId());
        row.setPaymentId(payment.getId());
        row.setCheckInDate(booking.getCheckInDate());
        row.setCheckOutDate(booking.getCheckOutDate());
        row.setNumberOfNights(nights);
        row.setBaseAmount(base);
        row.setGrossEarnings(base);
        row.setTaxesAndFees(payment.getTaxAmount() != null ? payment.getTaxAmount() : BigDecimal.ZERO);
        row.setCommissionRate(economicsProperties.getPlatformFeePercent());
        row.setCommissionAmount(payment.getPlatformFee() != null ? payment.getPlatformFee() : BigDecimal.ZERO);
        row.setNetEarnings(net);
        row.setStatus(HostEarningStatus.PENDING_RELEASE);
        row.setCurrency(economicsProperties.getCurrency());
        row.setNotes("Lodging earnings; released after check-out");

        hostEarningsRepository.save(row);
        log.info("Host earnings recorded payment {} booking {} status PENDING_RELEASE", payment.getId(), booking.getId());
    }

    @Transactional
    public void markRefundedForBooking(Long bookingId) {
        hostEarningsRepository.findByBookingId(bookingId).forEach(row -> {
            if (!HostEarningStatus.PAID.equals(row.getStatus())) {
                row.setStatus(HostEarningStatus.REFUNDED);
                hostEarningsRepository.save(row);
            }
        });
    }

    @Transactional
    public int releaseEligibleEarnings(LocalDate today) {
        List<HostEarnings> pending = hostEarningsRepository.findPendingReleaseBeforeCheckOut(today);
        for (HostEarnings row : pending) {
            row.setStatus(HostEarningStatus.EARNED);
            row.setEarnedDate(today);
            hostEarningsRepository.save(row);
        }
        if (!pending.isEmpty()) {
            log.info("Released {} host earnings rows to EARNED", pending.size());
        }
        return pending.size();
    }

    @Transactional(readOnly = true)
    public BigDecimal sumEarnedAvailable(Long hostId) {
        return zeroIfNull(hostEarningsRepository.sumNetByHostIdAndStatus(hostId, HostEarningStatus.EARNED));
    }

    @Transactional(readOnly = true)
    public BigDecimal sumPendingRelease(Long hostId) {
        return zeroIfNull(hostEarningsRepository.sumNetByHostIdAndStatus(hostId, HostEarningStatus.PENDING_RELEASE));
    }

    @Transactional
    public List<Long> allocateEarnedFifo(Long hostId, Long payoutId, BigDecimal amount) {
        List<HostEarnings> earned = hostEarningsRepository.findEarnedFifo(hostId);
        BigDecimal remaining = amount;
        List<Long> ids = new ArrayList<>();
        for (HostEarnings row : earned) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            if (row.getNetEarnings().compareTo(remaining) > 0) {
                break;
            }
            row.setStatus(HostEarningStatus.PAYOUT_PENDING);
            row.setPayoutId(payoutId);
            hostEarningsRepository.save(row);
            ids.add(row.getId());
            remaining = remaining.subtract(row.getNetEarnings());
        }
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalArgumentException("Insufficient earned balance to cover payout amount");
        }
        return ids;
    }

    @Transactional
    public void markPaidForPayout(Long payoutId) {
        hostEarningsRepository.findByPayoutId(payoutId).forEach(row -> {
            row.setStatus(HostEarningStatus.PAID);
            row.setPaidDate(LocalDate.now());
            hostEarningsRepository.save(row);
        });
    }

    @Transactional
    public void revertPayoutPending(Long payoutId) {
        hostEarningsRepository.findByPayoutId(payoutId).forEach(row -> {
            if (HostEarningStatus.PAYOUT_PENDING.equals(row.getStatus())) {
                row.setStatus(HostEarningStatus.EARNED);
                row.setPayoutId(null);
                hostEarningsRepository.save(row);
            }
        });
    }

    private static BigDecimal zeroIfNull(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
