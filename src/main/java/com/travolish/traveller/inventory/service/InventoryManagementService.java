package com.travolish.traveller.inventory.service;

import java.time.LocalDate;
import java.util.List;

import com.travolish.traveller.inventory.dto.InventoryAlertDTO;
import com.travolish.traveller.inventory.dto.InventoryDashboardDTO;
import com.travolish.traveller.inventory.dto.InventoryForecastDTO;
import com.travolish.traveller.inventory.dto.OccupancyReportDTO;
import com.travolish.traveller.inventory.dto.PricingCalculationDTO;
import com.travolish.traveller.inventory.dto.PricingRecommendationDTO;
import com.travolish.traveller.inventory.dto.PricingReportDTO;
import com.travolish.traveller.inventory.dto.RevenueForecastDTO;
import com.travolish.traveller.inventory.dto.RoomInventoryInitDTO;

public interface InventoryManagementService {

    /**
     * Complete inventory check before booking
     */
    Boolean canBookRoom(Long roomId, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Calculate final booking price with all pricing rules
     */
    PricingCalculationDTO calculateBookingPrice(
        Long roomId, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate, Double basePrice
    );

    /**
     * Process booking with inventory management
     */
    void processBooking(Long roomId, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Process booking cancellation
     */
    void processCancellation(Long roomId, LocalDate checkInDate, LocalDate checkOutDate);

    /**
     * Get real-time inventory dashboard for hotel
     */
    InventoryDashboardDTO getInventoryDashboard(Long hotelId, LocalDate date);

    /**
     * Get inventory forecast for date range
     */
    List<InventoryForecastDTO> getInventoryForecast(Long hotelId, LocalDate startDate, LocalDate endDate);

    /**
     * Generate pricing report for hotel in date range
     */
    PricingReportDTO generatePricingReport(Long hotelId, LocalDate startDate, LocalDate endDate);

    /**
     * Generate occupancy report
     */
    OccupancyReportDTO generateOccupancyReport(Long hotelId, LocalDate startDate, LocalDate endDate);

    /**
     * Generate revenue forecast
     */
    RevenueForecastDTO generateRevenueForecast(Long hotelId, LocalDate startDate, LocalDate endDate);

    /**
     * Get recommendations for pricing optimization
     */
    List<PricingRecommendationDTO> getPricingRecommendations(Long hotelId, LocalDate startDate, LocalDate endDate);

    /**
     * Get availability alerts (low stock, maintenance needed, etc.)
     */
    List<InventoryAlertDTO> getAvailabilityAlerts(Long hotelId);

    /**
     * Manual availability adjustment
     */
    void adjustAvailability(Long roomId, LocalDate date, Integer quantityChange, String reason);

    /**
     * Bulk availability update
     */
    void bulkUpdateAvailability(Long hotelId, LocalDate startDate, LocalDate endDate, Integer adjustment, String reason);

    /**
     * Initialize inventory for new hotel
     */
    void initializeHotelInventory(Long hotelId, List<RoomInventoryInitDTO> rooms, Integer daysAhead);
}

