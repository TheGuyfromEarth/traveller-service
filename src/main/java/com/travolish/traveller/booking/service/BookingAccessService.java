package com.travolish.traveller.booking.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.repository.HotelRepository;
import com.travolish.traveller.user.security.AuthSupport;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingAccessService {

    private final BookingRepository bookingRepository;
    private final HotelRepository hotelRepository;

    public Booking requireBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    /**
     * Guest who owns the booking, host of the property, or admin.
     */
    public void assertGuestHostOrAdmin(Long userId, boolean isAdmin, Long bookingId) {
        if (isAdmin) {
            return;
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        Booking booking = requireBooking(bookingId);
        if (booking.getUserId() != null && booking.getUserId().equals(userId)) {
            return;
        }
        if (booking.getHotelId() != null) {
            boolean hostMatch = hotelRepository.findById(booking.getHotelId())
                    .map(h -> h.getHostId() != null && h.getHostId().equals(userId))
                    .orElse(false);
            if (hostMatch) {
                return;
            }
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to access this booking");
    }

    public void assertGuestHostOrAdmin(org.springframework.security.core.Authentication authentication,
                                       Long bookingId) {
        assertGuestHostOrAdmin(
                AuthSupport.userId(authentication),
                AuthSupport.isAdmin(authentication),
                bookingId);
    }

    /** Booking must belong to the authenticated user (guest), unless admin. */
    public void assertBookingGuestOrAdmin(Long userId, boolean isAdmin, Booking booking) {
        if (isAdmin) {
            return;
        }
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        if (booking.getUserId() == null || !booking.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking does not belong to user");
        }
    }
}
