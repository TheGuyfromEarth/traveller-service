package com.travolish.traveller.analytics.controller;

import com.travolish.traveller.analytics.dto.*;
import com.travolish.traveller.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API endpoints for host dashboard and analytics
 */
@RestController
@RequestMapping("/api/host")
@RequiredArgsConstructor
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    /**
     * Get comprehensive dashboard overview
     * GET /api/host/dashboard/overview
     */
    @GetMapping("/dashboard/overview")
    public ResponseEntity<DashboardOverviewDTO> getDashboardOverview(@RequestParam Long hostId) {
        DashboardOverviewDTO overview = analyticsService.getDashboardOverview(hostId);
        return ResponseEntity.ok(overview);
    }
    
    /**
     * Get monthly bookings report
     * GET /api/host/bookings/monthly
     */
    @GetMapping("/bookings/monthly")
    public ResponseEntity<List<HostAnalyticsDTO>> getMonthlyBookings(
            @RequestParam Long hostId,
            @RequestParam(defaultValue = "1") Integer month,
            @RequestParam Integer year) {
        List<HostAnalyticsDTO> bookings = analyticsService.getMonthlyBookingsReport(hostId, month, year);
        return ResponseEntity.ok(bookings);
    }
    
    /**
     * Get earnings summary for current month
     * GET /api/host/earnings/summary
     */
    @GetMapping("/earnings/summary")
    public ResponseEntity<MonthlyEarningsSummaryDTO> getEarningsSummary(@RequestParam Long hostId) {
        MonthlyEarningsSummaryDTO summary = analyticsService.getEarningsSummary(hostId);
        return ResponseEntity.ok(summary);
    }
    
    /**
     * Get monthly earnings breakdown
     * GET /api/host/earnings/monthly
     */
    @GetMapping("/earnings/monthly")
    public ResponseEntity<List<MonthlyEarningsSummaryDTO>> getMonthlyEarningsBreakdown(
            @RequestParam Long hostId,
            @RequestParam(defaultValue = "12") Integer months) {
        List<MonthlyEarningsSummaryDTO> breakdown = analyticsService.getMonthlyEarningsBreakdown(hostId, months);
        return ResponseEntity.ok(breakdown);
    }
    
    /**
     * Get occupancy forecast
     * GET /api/host/occupancy/forecast
     */
    @GetMapping("/occupancy/forecast")
    public ResponseEntity<OccupancyForecastDTO> getOccupancyForecast(@RequestParam Long hostId) {
        OccupancyForecastDTO forecast = analyticsService.getOccupancyForecast(hostId);
        return ResponseEntity.ok(forecast);
    }
    
    /**
     * Get revenue analytics
     * GET /api/host/revenue/analytics
     */
    @GetMapping("/revenue/analytics")
    public ResponseEntity<DashboardOverviewDTO> getRevenueAnalytics(@RequestParam Long hostId) {
        DashboardOverviewDTO analytics = analyticsService.getRevenueAnalytics(hostId);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get review analytics
     * GET /api/host/reviews/analytics
     */
    @GetMapping("/reviews/analytics")
    public ResponseEntity<ReviewAnalyticsDTO> getReviewAnalytics(@RequestParam Long hostId) {
        ReviewAnalyticsDTO analytics = analyticsService.getReviewAnalytics(hostId);
        return ResponseEntity.ok(analytics);
    }
    
    /**
     * Get returning guests statistics
     * GET /api/host/guests/returning
     */
    @GetMapping("/guests/returning")
    public ResponseEntity<List<ReturningGuestDTO>> getReturningGuests(
            @RequestParam Long hostId,
            @RequestParam(defaultValue = "10") Integer limit) {
        List<ReturningGuestDTO> guests = analyticsService.getReturningGuests(hostId, limit);
        return ResponseEntity.ok(guests);
    }
    
    /**
     * Get earnings history (paginated)
     * GET /api/host/earnings/history
     */
    @GetMapping("/earnings/history")
    public ResponseEntity<Page<HostEarningsDTO>> getEarningsHistory(
            @RequestParam Long hostId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        Page<HostEarningsDTO> earnings = analyticsService.getEarningsHistory(hostId, page, pageSize);
        return ResponseEntity.ok(earnings);
    }
    
    /**
     * Record a new earning (for internal use)
     * POST /api/host/earnings
     */
    @PostMapping("/earnings")
    public ResponseEntity<HostEarningsDTO> recordEarning(
            @RequestParam Long hostId,
            @RequestBody HostEarningsDTO earningDTO) {
        HostEarningsDTO recorded = analyticsService.recordEarning(hostId, earningDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(recorded);
    }
}
