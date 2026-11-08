package com.travolish.traveller.inventory.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.travolish.traveller.inventory.dto.PricingCalculationDTO;
import com.travolish.traveller.inventory.service.InventoryManagementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryManagementService inventoryManagementService;

    /**
     * Check if room can be booked for date range
     */
    @GetMapping("/can-book")
    public ResponseEntity<Boolean> canBookRoom(
        @RequestParam Long roomId,
        @RequestParam Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        
        Boolean canBook = inventoryManagementService.canBookRoom(roomId, hotelId, checkInDate, checkOutDate);
        return ResponseEntity.ok(canBook);
    }

    /**
     * Calculate booking price with all pricing rules
     */
    @GetMapping("/calculate-price")
    public ResponseEntity<PricingCalculationDTO> calculateBookingPrice(
        @RequestParam Long roomId,
        @RequestParam Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate,
        @RequestParam Double basePrice) {
        
        PricingCalculationDTO calculation = inventoryManagementService
            .calculateBookingPrice(roomId, hotelId, checkInDate, checkOutDate, basePrice);
        return ResponseEntity.ok(calculation);
    }

    /**
     * Process booking with inventory management
     */
    @PostMapping("/book")
    @ResponseStatus(HttpStatus.OK)
    public void processBooking(
        @RequestParam Long roomId,
        @RequestParam Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        
        inventoryManagementService.processBooking(roomId, hotelId, checkInDate, checkOutDate);
    }

    /**
     * Process booking cancellation
     */
    @PostMapping("/cancel-booking")
    @ResponseStatus(HttpStatus.OK)
    public void processCancellation(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkInDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOutDate) {
        
        inventoryManagementService.processCancellation(roomId, checkInDate, checkOutDate);
    }

    /**
     * Get real-time inventory dashboard for hotel
     */
    @GetMapping("/dashboard/{hotelId}")
    public ResponseEntity<Object> getInventoryDashboard(
        @PathVariable Long hotelId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        Object dashboard = inventoryManagementService.getInventoryDashboard(hotelId, targetDate);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get inventory forecast for date range
     */
    @GetMapping("/forecast/{hotelId}")
    public ResponseEntity<List<Object>> getInventoryForecast(
        @PathVariable Long hotelId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : LocalDate.now().plusDays(30);
                @SuppressWarnings("unchecked")        List<Object> forecast = (List<Object>) (List<?>) inventoryManagementService
            .getInventoryForecast(hotelId, start, end);
        return ResponseEntity.ok(forecast);
    }

    /**
     * Generate pricing report for hotel
     */
    @GetMapping("/reports/pricing/{hotelId}")
    public ResponseEntity<Object> generatePricingReport(
        @PathVariable Long hotelId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : LocalDate.now().plusDays(30);
        
        Object report = inventoryManagementService.generatePricingReport(hotelId, start, end);
        return ResponseEntity.ok(report);
    }

    /**
     * Generate occupancy report
     */
    @GetMapping("/reports/occupancy/{hotelId}")
    public ResponseEntity<Object> generateOccupancyReport(
        @PathVariable Long hotelId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : LocalDate.now().plusDays(30);
        
        Object report = inventoryManagementService.generateOccupancyReport(hotelId, start, end);
        return ResponseEntity.ok(report);
    }

    /**
     * Generate revenue forecast
     */
    @GetMapping("/reports/revenue/{hotelId}")
    public ResponseEntity<Object> generateRevenueForecast(
        @PathVariable Long hotelId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : LocalDate.now().plusDays(30);
        
        Object forecast = inventoryManagementService.generateRevenueForecast(hotelId, start, end);
        return ResponseEntity.ok(forecast);
    }

    /**
     * Get pricing recommendations
     */
    @GetMapping("/recommendations/{hotelId}")
    public ResponseEntity<List<Object>> getPricingRecommendations(
        @PathVariable Long hotelId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        LocalDate start = startDate != null ? startDate : LocalDate.now();
        LocalDate end = endDate != null ? endDate : LocalDate.now().plusDays(30);
        
        @SuppressWarnings("unchecked")
        List<Object> recommendations = (List<Object>) (List<?>) inventoryManagementService
            .getPricingRecommendations(hotelId, start, end);
        return ResponseEntity.ok(recommendations);
    }

    /**
     * Get availability alerts
     */
    @GetMapping("/alerts/{hotelId}")
    public ResponseEntity<List<Object>> getAvailabilityAlerts(@PathVariable Long hotelId) {
        @SuppressWarnings("unchecked")
        List<Object> alerts = (List<Object>) (List<?>) inventoryManagementService.getAvailabilityAlerts(hotelId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Manually adjust availability
     */
    @PostMapping("/adjust")
    @ResponseStatus(HttpStatus.OK)
    public void adjustAvailability(
        @RequestParam Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam Integer quantityChange,
        @RequestParam(required = false) String reason) {
        
        inventoryManagementService.adjustAvailability(roomId, date, quantityChange, reason);
    }

    /**
     * Bulk availability update
     */
    @PostMapping("/bulk-adjust")
    @ResponseStatus(HttpStatus.OK)
    public void bulkUpdateAvailability(
        @RequestParam Long hotelId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam Integer adjustment,
        @RequestParam(required = false) String reason) {
        
        inventoryManagementService.bulkUpdateAvailability(hotelId, startDate, endDate, adjustment, reason);
    }
}
