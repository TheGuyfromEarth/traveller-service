package com.travolish.traveller.inventory.service;

import java.time.LocalDate;
import java.util.List;

import com.travolish.traveller.inventory.dto.AvailabilityCheckDTO;

public interface AvailabilityService {

    /**
     * Check if room is available for specific date
     */
    Boolean isRoomAvailableOnDate(Long roomId, LocalDate date);

    /**
     * Check if room is available for date range (all dates must be available)
     */
    Boolean isRoomAvailableForDateRange(Long roomId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Get availability for specific room on specific date
     */
    AvailabilityCheckDTO getAvailabilityForDate(Long roomId, LocalDate date);

    /**
     * Get availability for room for entire date range
     */
    List<AvailabilityCheckDTO> getAvailabilityForDateRange(
        Long roomId, LocalDate checkInDate, LocalDate checkOutDate
    );

    /**
     * Find available rooms for hotel on specific date
     */
    List<AvailabilityCheckDTO> findAvailableRoomsOnDate(Long hotelId, LocalDate date);

    /**
     * Find available rooms for hotel in date range
     */
    List<Long> findAvailableRoomsInDateRange(
        Long hotelId, LocalDate checkInDate, LocalDate checkOutDate
    );

    /**
     * Book a room (reduce available count)
     */
    void bookRoom(Long roomId, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Cancel booking (increase available count)
     */
    void cancelBooking(Long roomId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Block rooms for maintenance. hotelId is used to auto-create the record when none exists yet.
     */
    void blockRoomsForMaintenance(Long roomId, LocalDate date, Integer count, String reason, Long hotelId);

    /**
     * Unblock rooms. hotelId is used to auto-create the record when none exists yet.
     */
    void unblockRooms(Long roomId, LocalDate date, Integer count, Long hotelId);

    /**
     * Initialize room availability for future dates (typically 1-2 years ahead)
     */
    void initializeRoomAvailability(Long hotelId, Long roomId, Integer roomCount, Integer daysAhead);

    /**
     * Get occupancy stats for hotel on specific date
     */
    AvailabilityCheckDTO getHotelOccupancyOnDate(Long hotelId, LocalDate date);

    /**
     * Get occupancy stats for hotel in date range
     */
    List<AvailabilityCheckDTO> getHotelOccupancyForDateRange(
        Long hotelId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Calculate average occupancy percentage for hotel in date range
     */
    Double calculateAverageOccupancy(Long hotelId, LocalDate startDate, LocalDate endDate);

    /**
     * Check for booking conflicts (date overlap)
     */
    Boolean hasBookingConflict(Long roomId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Update room availability status based on current bookings
     */
    void updateAvailabilityStatus(Long roomId, LocalDate date);

    /**
     * Get rooms with limited availability (less than 20%)
     */
    List<AvailabilityCheckDTO> getRoomsWithLimitedAvailability(Long hotelId, LocalDate date);

    /**
     * Clean up old availability records (for archiving)
     */
    Long archiveOldAvailabilityRecords(LocalDate beforeDate);
}
