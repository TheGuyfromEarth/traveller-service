package com.travolish.traveller.inventory.service;

import java.time.LocalDate;
import java.util.List;

import com.travolish.traveller.inventory.dto.DemandMetricsDTO;
import com.travolish.traveller.inventory.model.DemandMetrics.DemandLevel;

public interface DynamicPricingService {

    /**
     * Calculate price based on current demand
     */
    Double calculateDynamicPrice(Long roomId, LocalDate date);

    /**
     * Calculate price for date range with demand-based adjustments
     */
    Double calculatePriceForDateRange(Long roomId, LocalDate checkInDate, LocalDate checkOutDate, Double basePrice);

    /**
     * Get demand level for specific date
     */
    DemandLevel getDemandLevel(Long roomId, LocalDate date);

    /**
     * Get price multiplier based on occupancy
     */
    Double getPriceMultiplier(Long roomId, LocalDate date);

    /**
     * Calculate price based on occupancy percentage
     */
    Double calculatePriceByOccupancy(Double basePrice, Double occupancyPercentage);

    /**
     * Calculate price based on booking velocity
     */
    Double calculatePriceByVelocity(Double basePrice, Double bookingVelocity, Integer daysUntilDate);

    /**
     * Record demand metrics for date
     */
    DemandMetricsDTO recordDemandMetrics(DemandMetricsDTO metricsDTO);

    /**
     * Update demand metrics based on current occupancy
     */
    void updateDemandMetrics(Long roomId, LocalDate date);

    /**
     * Get all demand metrics for room in date range
     */
    List<DemandMetricsDTO> getDemandMetricsForDateRange(
        Long roomId, LocalDate startDate, LocalDate endDate
    );

    /**
     * Find high demand periods
     */
    List<DemandMetricsDTO> findHighDemandPeriods(Long roomId, LocalDate startDate, LocalDate endDate);

    /**
     * Find low demand periods (for discount pricing)
     */
    List<DemandMetricsDTO> findLowDemandPeriods(Long roomId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculate average occupancy for room
     */
    Double getAverageOccupancy(Long roomId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculate cancellation rate
     */
    Double getCancellationRate(Long roomId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculate booking conversion rate
     */
    Double getConversionRate(Long roomId, LocalDate startDate, LocalDate endDate);

    /**
     * Predict price for future date based on historical demand
     */
    Double predictPriceForDate(Long roomId, LocalDate date);

    /**
     * Get trending demand direction
     */
    String getTrendingDemandDirection(Long roomId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculate optimal price that maximizes revenue
     */
    Double calculateOptimalPrice(Long roomId, LocalDate date, Double basePrice);

    /**
     * Clean up old demand metrics (for archiving)
     */
    Long archiveOldDemandMetrics(LocalDate beforeDate);
}
