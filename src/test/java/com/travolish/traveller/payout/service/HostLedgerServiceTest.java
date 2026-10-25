package com.travolish.traveller.payout.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travolish.traveller.analytics.entity.HostEarnings;
import com.travolish.traveller.analytics.repository.HostEarningsRepository;
import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.config.PlatformEconomicsProperties;
import com.travolish.traveller.hotel.model.Hotel;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.payment.entity.Payment;
import com.travolish.traveller.payout.HostEarningStatus;

@ExtendWith(MockitoExtension.class)
class HostLedgerServiceTest {

    @Mock private HostEarningsRepository hostEarningsRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private HotelRepository hotelRepository;
    @Mock private PlatformEconomicsProperties economicsProperties;

    @InjectMocks
    private HostLedgerService hostLedgerService;

    @Test
    void releaseEligibleEarnings_movesPendingReleaseToEarned() {
        HostEarnings row = new HostEarnings();
        row.setId(1L);
        row.setStatus(HostEarningStatus.PENDING_RELEASE);
        when(hostEarningsRepository.findPendingReleaseBeforeCheckOut(LocalDate.of(2026, 1, 15)))
            .thenReturn(List.of(row));
        when(hostEarningsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int count = hostLedgerService.releaseEligibleEarnings(LocalDate.of(2026, 1, 15));

        assertEquals(1, count);
        assertEquals(HostEarningStatus.EARNED, row.getStatus());
        assertEquals(LocalDate.of(2026, 1, 15), row.getEarnedDate());
    }

    @Test
    void recordPaymentCaptured_usesNetAmountAndPendingRelease() {
        when(economicsProperties.getCurrency()).thenReturn("INR");
        when(economicsProperties.getPlatformFeePercent()).thenReturn(new BigDecimal("2.5"));
        when(hostEarningsRepository.findByPaymentId(99L)).thenReturn(Optional.empty());

        Payment payment = new Payment();
        payment.setId(99L);
        payment.setBookingId(5L);
        payment.setBaseAmount(new BigDecimal("1000"));
        payment.setNetAmount(new BigDecimal("1000"));
        payment.setPlatformFee(new BigDecimal("25"));

        Booking booking = new Booking();
        booking.setId(5L);
        booking.setHotelId(7L);
        booking.setRoomId(3L);
        booking.setUserId(2L);
        booking.setCheckInDate(LocalDate.of(2026, 1, 1));
        booking.setCheckOutDate(LocalDate.of(2026, 1, 3));

        Hotel hotel = new Hotel();
        hotel.setId(7L);
        hotel.setHostId(42L);

        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(hotelRepository.findById(7L)).thenReturn(Optional.of(hotel));
        when(hostEarningsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        hostLedgerService.recordPaymentCaptured(payment);

        verify(hostEarningsRepository).save(any(HostEarnings.class));
    }
}
