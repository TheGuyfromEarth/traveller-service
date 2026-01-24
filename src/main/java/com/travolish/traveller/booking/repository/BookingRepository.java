package com.travolish.traveller.booking.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.booking.model.Booking;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Find all bookings for a specific room
     */
    List<Booking> findByRoomId(Long roomId);

    /**
     * Find all bookings for a specific hotel
     */
    List<Booking> findByHotelId(Long hotelId);

    /**
     * Find all bookings for a specific room and status
     */
    List<Booking> findByRoomIdAndStatus(Long roomId, Booking.BookingStatus status);

    /**
     * Find all bookings for a date range that overlap with the given dates
     * Confirmed or Pending bookings only
     */
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId " +
           "AND b.status IN (com.travolish.traveller.booking.model.Booking.BookingStatus.CONFIRMED, " +
           "com.travolish.traveller.booking.model.Booking.BookingStatus.PENDING) " +
           "AND (b.checkInDate < :checkOutDate AND b.checkOutDate > :checkInDate)")
    List<Booking> findConflictingBookings(@Param("roomId") Long roomId,
                                          @Param("checkInDate") LocalDate checkInDate,
                                          @Param("checkOutDate") LocalDate checkOutDate);

    /**
     * Find all confirmed bookings for a room in a date range
     */
    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId " +
           "AND b.status = com.travolish.traveller.booking.model.Booking.BookingStatus.CONFIRMED " +
           "AND b.checkInDate >= :startDate AND b.checkOutDate <= :endDate")
    List<Booking> findConfirmedBookingsInDateRange(@Param("roomId") Long roomId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /**
     * Find all bookings for a hotel in a date range
     */
    @Query("SELECT b FROM Booking b WHERE b.hotelId = :hotelId " +
           "AND b.status = com.travolish.traveller.booking.model.Booking.BookingStatus.CONFIRMED " +
           "AND b.checkInDate >= :startDate AND b.checkOutDate <= :endDate")
    List<Booking> findConfirmedBookingsForHotelInDateRange(@Param("hotelId") Long hotelId,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);

    /**
     * Get total bookings count for a room
     */
    long countByRoomIdAndStatus(Long roomId, Booking.BookingStatus status);

    /**
     * Get total revenue from confirmed bookings for a hotel
     */
    @Query("SELECT SUM(b.totalPrice) FROM Booking b WHERE b.hotelId = :hotelId " +
           "AND b.status = com.travolish.traveller.booking.model.Booking.BookingStatus.CONFIRMED")
    Double getTotalRevenueForHotel(@Param("hotelId") Long hotelId);

    /**
     * Get average booking value for a room
     */
    @Query("SELECT AVG(b.totalPrice) FROM Booking b WHERE b.roomId = :roomId " +
           "AND b.status = com.travolish.traveller.booking.model.Booking.BookingStatus.CONFIRMED")
    Double getAverageBookingValueForRoom(@Param("roomId") Long roomId);
}
