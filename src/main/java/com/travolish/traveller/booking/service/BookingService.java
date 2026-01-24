package com.travolish.traveller.booking.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
     * Cancel a booking by ID
     */
    void cancelBooking(Long bookingId);
}
