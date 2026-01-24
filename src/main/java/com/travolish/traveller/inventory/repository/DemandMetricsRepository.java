package com.travolish.traveller.inventory.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.travolish.traveller.inventory.model.DemandMetrics;
import com.travolish.traveller.inventory.model.DemandMetrics.DemandLevel;

@Repository
public interface DemandMetricsRepository extends JpaRepository<DemandMetrics, Long> {

    /**
     * Find demand metrics for specific room on specific date
     */
    Optional<DemandMetrics> findByRoomIdAndMetricDate(Long roomId, LocalDate date);

    /**
     * Find demand metrics for room in date range
     */
    List<DemandMetrics> findByRoomIdAndMetricDateBetween(
        Long roomId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Find demand metrics for hotel in date range
     */
    List<DemandMetrics> findByHotelIdAndMetricDateBetween(
        Long hotelId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Find high demand dates (VERY_HIGH demand level)
     */
    @Query("SELECT dm FROM DemandMetrics dm WHERE dm.roomId = :roomId " +
           "AND dm.demandLevel = com.travolish.traveller.inventory.model.DemandMetrics$DemandLevel.VERY_HIGH " +
           "AND dm.metricDate BETWEEN :startDate AND :endDate")
    List<DemandMetrics> findHighDemandDates(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find low demand dates for discount pricing
     */
    @Query("SELECT dm FROM DemandMetrics dm WHERE dm.roomId = :roomId " +
           "AND dm.demandLevel = com.travolish.traveller.inventory.model.DemandMetrics$DemandLevel.LOW " +
           "AND dm.metricDate BETWEEN :startDate AND :endDate")
    List<DemandMetrics> findLowDemandDates(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find metrics with specific demand level
     */
    List<DemandMetrics> findByDemandLevel(DemandLevel demandLevel);

    /**
     * Find rooms with highest occupancy
     */
    @Query(value = "SELECT * FROM demand_metrics WHERE hotel_id = :hotelId " +
           "AND metric_date BETWEEN :startDate AND :endDate " +
           "ORDER BY occupancy_rate DESC LIMIT :limit", nativeQuery = true)
    List<DemandMetrics> findMostOccupiedRooms(
        @Param("hotelId") Long hotelId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("limit") int limit
    );

    /**
     * Calculate average occupancy rate for room in date range
     */
    @Query("SELECT AVG(dm.occupancyRate) FROM DemandMetrics dm " +
           "WHERE dm.roomId = :roomId AND dm.metricDate BETWEEN :startDate AND :endDate")
    Optional<Double> getAverageOccupancyRate(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate total bookings for room in date range
     */
    @Query("SELECT SUM(dm.bookingsCount) FROM DemandMetrics dm " +
           "WHERE dm.roomId = :roomId AND dm.metricDate BETWEEN :startDate AND :endDate")
    Optional<Integer> getTotalBookingsInRange(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Calculate cancellation rate for room
     */
    @Query("SELECT COALESCE(SUM(dm.cancelledCount) * 100.0 / SUM(dm.bookingsCount), 0) " +
           "FROM DemandMetrics dm WHERE dm.roomId = :roomId " +
           "AND dm.metricDate BETWEEN :startDate AND :endDate")
    Double getCancellationRateForRoom(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get trending demand (comparing two date ranges)
     */
    @Query("SELECT AVG(dm.occupancyRate) FROM DemandMetrics dm " +
           "WHERE dm.roomId = :roomId AND dm.metricDate BETWEEN :startDate AND :endDate")
    Optional<Double> getOccupancyTrend(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find metrics for cleanup/archiving (older than specified date)
     */
    List<DemandMetrics> findByMetricDateBefore(LocalDate date);

    /**
     * Delete old metrics for archiving
     */
    Long deleteByMetricDateBefore(LocalDate date);

    /**
     * Get total view count for room in date range
     */
    @Query("SELECT SUM(dm.viewCount) FROM DemandMetrics dm " +
           "WHERE dm.roomId = :roomId AND dm.metricDate BETWEEN :startDate AND :endDate")
    Optional<Long> getTotalViewsInRange(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Get conversion rate (bookings vs views)
     */
    @Query("SELECT COALESCE(SUM(dm.bookingsCount) * 100.0 / SUM(dm.viewCount), 0) " +
           "FROM DemandMetrics dm WHERE dm.roomId = :roomId " +
           "AND dm.metricDate BETWEEN :startDate AND :endDate")
    Double getConversionRate(
        @Param("roomId") Long roomId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
}
