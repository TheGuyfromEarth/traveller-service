package com.travolish.traveller.inventory.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.inventory.model.RoomAvailability;
import com.travolish.traveller.inventory.model.RoomAvailability.AvailabilityStatus;

@Repository
public interface RoomAvailabilityRepository extends JpaRepository<RoomAvailability, Long> {

    /**
     * Find availability for specific room on specific date
     */
    Optional<RoomAvailability> findByRoomIdAndAvailabilityDate(Long roomId, LocalDate date);

    /**
     * Find all availability records for a room in date range
     */
    List<RoomAvailability> findByRoomIdAndAvailabilityDateBetween(
        Long roomId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Same as above but with a pessimistic write lock — use inside a booking transaction
     * to prevent concurrent overbooking (TOCTOU protection).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ra FROM RoomAvailability ra WHERE ra.roomId = :roomId " +
           "AND ra.availabilityDate BETWEEN :startDate AND :endDate")
    List<RoomAvailability> findByRoomIdAndDateRangeWithLock(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find all availability records for a hotel in date range
     */
    List<RoomAvailability> findByHotelIdAndAvailabilityDateBetween(
        Long hotelId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Find all availability records for a room in date range with specific status
     */
    List<RoomAvailability> findByRoomIdAndAvailabilityDateBetweenAndStatus(
        Long roomId, LocalDate startDate, LocalDate endDate, AvailabilityStatus status
    );

    /**
     * Find all rooms with available inventory on specific date
     */
    @Query("SELECT ra FROM RoomAvailability ra WHERE ra.availabilityDate = :date " +
           "AND ra.availableRooms > 0 AND ra.status = com.travolish.traveller.inventory.model.RoomAvailability$AvailabilityStatus.AVAILABLE")
    List<RoomAvailability> findAvailableRoomsOnDate(@Param("date") LocalDate date);

    /**
     * Find all rooms available in date range (all dates must have availability)
     */
    @Query("SELECT ra.roomId FROM RoomAvailability ra " +
           "WHERE ra.hotelId = :hotelId " +
           "AND ra.availabilityDate BETWEEN :startDate AND :endDate " +
           "AND ra.availableRooms > 0 " +
           "GROUP BY ra.roomId " +
           "HAVING COUNT(ra.id) = :dayCount")
    List<Long> findAvailableRoomsInDateRange(
        @Param("hotelId") Long hotelId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("dayCount") long dayCount
    );

    /**
     * Find rooms with limited availability (less than 20% available)
     */
    List<RoomAvailability> findByHotelIdAndAvailabilityDateAndStatus(
        Long hotelId, LocalDate date, AvailabilityStatus status
    );

    /**
     * Find fully booked rooms on specific date
     */
    @Query("SELECT ra FROM RoomAvailability ra WHERE ra.hotelId = :hotelId " +
           "AND ra.availabilityDate = :date AND ra.availableRooms = 0")
    List<RoomAvailability> findFullyBookedRoomsOnDate(
        @Param("hotelId") Long hotelId,
        @Param("date") LocalDate date
    );

    /**
     * Calculate average occupancy for hotel in date range
     */
    @Query("SELECT AVG((ra.bookedRooms * 100.0 / ra.totalRooms)) FROM RoomAvailability ra " +
           "WHERE ra.hotelId = :hotelId AND ra.availabilityDate BETWEEN :startDate AND :endDate")
    Optional<Double> calculateAverageOccupancy(
        @Param("hotelId") Long hotelId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find rooms with highest demand (highest booked count)
     */
    @Query(value = "SELECT * FROM room_availability WHERE hotel_id = :hotelId " +
           "AND availability_date BETWEEN :startDate AND :endDate " +
           "ORDER BY booked_rooms DESC LIMIT :limit", nativeQuery = true)
    List<RoomAvailability> findMostBookedRooms(
        @Param("hotelId") Long hotelId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("limit") int limit
    );

    /**
     * Find availability records for cleanup/archiving (older than specified date)
     */
    List<RoomAvailability> findByAvailabilityDateBefore(LocalDate date);

    /**
     * Delete old availability records for archiving
     */
    Long deleteByAvailabilityDateBefore(LocalDate date);

    /**
     * Get total available rooms for hotel on specific date
     */
    @Query("SELECT SUM(ra.availableRooms) FROM RoomAvailability ra " +
           "WHERE ra.hotelId = :hotelId AND ra.availabilityDate = :date")
    Optional<Integer> getTotalAvailableRoomsOnDate(
        @Param("hotelId") Long hotelId,
        @Param("date") LocalDate date
    );

    /**
     * Get total booked rooms for hotel on specific date
     */
    @Query("SELECT SUM(ra.bookedRooms) FROM RoomAvailability ra " +
           "WHERE ra.hotelId = :hotelId AND ra.availabilityDate = :date")
    Optional<Integer> getTotalBookedRoomsOnDate(
        @Param("hotelId") Long hotelId,
        @Param("date") LocalDate date
    );
}
