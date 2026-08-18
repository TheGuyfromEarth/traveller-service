package com.travolish.traveller.booking.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.travolish.traveller.booking.model.Booking;
import com.travolish.traveller.booking.dto.BookingPriceDTO;

public interface BookingService {
    List<Booking> findAll();

    Optional<Booking> findById(Long id);

    Booking create(Booking booking);

    Optional<Booking> update(Long id, Booking booking);

    void delete(Long id);

    /**
     * Check if a room is available for booking in the given date range
     */
    Boolean checkAvailability(Long roomId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Calculate the price for a booking without creating it
     */
    BookingPriceDTO calculateBookingPrice(Long roomId, Double basePrice, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Get all bookings for a specific room
     */
    List<Booking> findByRoomId(Long roomId);

    /**
     * Get all bookings for a specific hotel
     */
    List<Booking> findByHotelId(Long hotelId);

    /**
     * Get all confirmed bookings for a date range
     */
    List<Booking> findConfirmedBookingsInDateRange(Long roomId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Cancel a booking by ID.
     * Returns {@code true} if the booking was found and cancelled,
     * {@code false} if no booking exists for the given ID.
     */
    boolean cancelBooking(Long bookingId);

    /**
     * Get all bookings for a specific guest email
     */
    List<Booking> findByGuestEmail(String guestEmail);

    List<Booking> findByUserId(Long userId);

    Page<Booking> findAdminBookings(String searchPattern, Booking.BookingStatus status, Pageable pageable);
}
