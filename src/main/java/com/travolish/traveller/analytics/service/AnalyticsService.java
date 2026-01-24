package com.travolish.traveller.analytics.service;

import com.travolish.traveller.analytics.dto.*;
import com.travolish.traveller.analytics.entity.HostAnalytics;
import com.travolish.traveller.analytics.entity.HostEarnings;
import com.travolish.traveller.analytics.repository.HostAnalyticsRepository;
import com.travolish.traveller.analytics.repository.HostEarningsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AnalyticsService {
    
    private final HostAnalyticsRepository hostAnalyticsRepository;
    private final HostEarningsRepository hostEarningsRepository;
    private final ModelMapper modelMapper;
    
    // ========== Dashboard & Overview Methods ==========
    
    /**
     * Get comprehensive dashboard overview for a host
     */
    public DashboardOverviewDTO getDashboardOverview(Long hostId) {
        log.debug("Fetching dashboard overview for host: {}", hostId);
        
        DashboardOverviewDTO overview = new DashboardOverviewDTO();
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate startOfLastMonth = startOfMonth.minusMonths(1);
        
        // Get current month analytics
        List<HostAnalytics> monthAnalytics = hostAnalyticsRepository
            .findByHostIdAndDateRange(hostId, startOfMonth, today);
        
        List<HostAnalytics> lastMonthAnalytics = hostAnalyticsRepository
            .findByHostIdAndDateRange(hostId, startOfLastMonth, startOfMonth.minusDays(1));
        
        // Calculate summary stats
        BigDecimal totalEarnings = hostEarningsRepository.getTotalPaidEarnings(hostId);
        overview.setTotalEarnings(totalEarnings != null ? totalEarnings : BigDecimal.ZERO);
        
        BigDecimal monthEarnings = hostEarningsRepository.getMonthlyEarnings(
            hostId, today.getMonthValue(), today.getYear()
        );
        overview.setThisMonthEarnings(monthEarnings != null ? monthEarnings : BigDecimal.ZERO);
        
        if (!monthAnalytics.isEmpty()) {
            HostAnalytics latestAnalytics = monthAnalytics.get(0);
            overview.setTotalBookings(latestAnalytics.getTotalBookings());
            overview.setAverageRating(latestAnalytics.getAverageRating());
            overview.setOccupancyRate(latestAnalytics.getOccupancyRate());
        }
        
        // Trending data
        overview.setBookingsTrend(generateBookingsTrend(hostId, thirtyDaysAgo, today));
        overview.setRevenueTrend(generateRevenueTrend(hostId, thirtyDaysAgo, today));
        overview.setOccupancyTrend(generateOccupancyTrend(hostId, thirtyDaysAgo, today));
        
        // Recent activities
        List<HostEarnings> thisMonthEarnings = hostEarningsRepository
            .findByHostIdAndDateRange(hostId, startOfMonth, today);
        overview.setThisMonthBookings((int) thisMonthEarnings.stream()
            .filter(he -> "PAID".equals(he.getStatus()) || "EARNED".equals(he.getStatus()))
            .count());
        
        overview.setCancelledThisMonth((int) thisMonthEarnings.stream()
            .filter(he -> "CANCELLED".equals(he.getStatus()))
            .count());
        
        overview.setPendingBookings((int) thisMonthEarnings.stream()
            .filter(he -> "PENDING".equals(he.getStatus()))
            .count());
        
        if (!monthAnalytics.isEmpty()) {
            overview.setResponseRate(monthAnalytics.get(0).getResponseRate());
            overview.setCancellationRate(monthAnalytics.get(0).getCancellationRate());
        }
        
        return overview;
    }
    
    // ========== Monthly Analytics Methods ==========
    
    /**
     * Get monthly bookings report
     */
    public List<HostAnalyticsDTO> getMonthlyBookingsReport(Long hostId, Integer month, Integer year) {
        log.debug("Fetching monthly bookings report for host: {} - {}/{}", hostId, month, year);
        
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        
        List<HostAnalytics> analytics = hostAnalyticsRepository
            .findByHostIdAndDateRange(hostId, startDate, endDate);
        
        return analytics.stream()
            .map(a -> modelMapper.map(a, HostAnalyticsDTO.class))
            .collect(Collectors.toList());
    }
    
    /**
     * Get earnings summary
     */
    public MonthlyEarningsSummaryDTO getEarningsSummary(Long hostId) {
        log.debug("Fetching earnings summary for host: {}", hostId);
        
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();
        
        MonthlyEarningsSummaryDTO summary = new MonthlyEarningsSummaryDTO();
        summary.setMonth(month);
        summary.setYear(year);
        
        BigDecimal monthlyEarnings = hostEarningsRepository.getMonthlyEarnings(hostId, month, year);
        summary.setTotalEarnings(monthlyEarnings != null ? monthlyEarnings : BigDecimal.ZERO);
        
        // Get detailed breakdown
        LocalDate startOfMonth = today.withDayOfMonth(1);
        List<HostEarnings> monthEarnings = hostEarningsRepository
            .findByHostIdAndDateRange(hostId, startOfMonth, today);
        
        summary.setCompletedBookings(monthEarnings.size());
        
        BigDecimal grossRevenue = monthEarnings.stream()
            .map(HostEarnings::getGrossEarnings)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setGrossRevenue(grossRevenue);
        
        BigDecimal commissionPaid = monthEarnings.stream()
            .map(HostEarnings::getCommissionAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        summary.setCommissionPaid(commissionPaid);
        
        summary.setNetEarnings(monthlyEarnings != null ? monthlyEarnings : BigDecimal.ZERO);
        
        if (!monthEarnings.isEmpty()) {
            BigDecimal avgBookingValue = grossRevenue.divide(
                BigDecimal.valueOf(monthEarnings.size()), 2, java.math.RoundingMode.HALF_UP
            );
            summary.setAverageBookingValue(avgBookingValue);
            
            Set<Long> uniqueGuests = monthEarnings.stream()
                .map(HostEarnings::getGuestId)
                .collect(Collectors.toSet());
            summary.setTotalGuests(uniqueGuests.size());
        }
        
        return summary;
    }
    
    /**
     * Get monthly earnings breakdown
     */
    public List<MonthlyEarningsSummaryDTO> getMonthlyEarningsBreakdown(Long hostId, Integer months) {
        log.debug("Fetching {}-month earnings breakdown for host: {}", months, hostId);
        
        List<MonthlyEarningsSummaryDTO> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        for (int i = 0; i < months; i++) {
            YearMonth yearMonth = YearMonth.from(today.minusMonths(i));
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();
            
            List<HostEarnings> earnings = hostEarningsRepository
                .findByHostIdAndDateRange(hostId, startDate, endDate);
            
            MonthlyEarningsSummaryDTO summary = new MonthlyEarningsSummaryDTO();
            summary.setMonth(yearMonth.getMonthValue());
            summary.setYear(yearMonth.getYear());
            summary.setCompletedBookings(earnings.size());
            
            BigDecimal totalEarnings = earnings.stream()
                .map(HostEarnings::getNetEarnings)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            summary.setTotalEarnings(totalEarnings);
            
            result.add(summary);
        }
        
        return result;
    }
    
    // ========== Occupancy & Forecasting Methods ==========
    
    /**
     * Get occupancy forecast
     */
    public OccupancyForecastDTO getOccupancyForecast(Long hostId) {
        log.debug("Generating occupancy forecast for host: {}", hostId);
        
        OccupancyForecastDTO forecast = new OccupancyForecastDTO();
        LocalDate today = LocalDate.now();
        LocalDate next30Days = today.plusDays(30);
        
        List<HostAnalytics> analytics = hostAnalyticsRepository
            .findByHostIdAndDateRange(hostId, today, next30Days);
        
        if (!analytics.isEmpty()) {
            HostAnalytics latestAnalytics = analytics.get(0);
            forecast.setForecastedOccupancy(latestAnalytics.getOccupancyRate());
            forecast.setBookedNights(latestAnalytics.getOccupiedNights());
            forecast.setAvailableNights(latestAnalytics.getTotalAvailableNights());
        }
        
        forecast.setDaysAhead(30);
        
        // Price recommendation based on occupancy
        if (forecast.getForecastedOccupancy() != null) {
            if (forecast.getForecastedOccupancy().compareTo(BigDecimal.valueOf(80)) > 0) {
                forecast.setPriceRecommendation("increase");
                forecast.setDemandLevel(5);
            } else if (forecast.getForecastedOccupancy().compareTo(BigDecimal.valueOf(60)) > 0) {
                forecast.setPriceRecommendation("maintain");
                forecast.setDemandLevel(4);
            } else {
                forecast.setPriceRecommendation("decrease");
                forecast.setDemandLevel(2);
            }
        }
        
        return forecast;
    }
    
    // ========== Revenue Analytics Methods ==========
    
    /**
     * Get revenue analytics
     */
    public DashboardOverviewDTO getRevenueAnalytics(Long hostId) {
        log.debug("Fetching revenue analytics for host: {}", hostId);
        return getDashboardOverview(hostId);
    }
    
    // ========== Review Analytics Methods ==========
    
    /**
     * Get review analytics
     */
    public ReviewAnalyticsDTO getReviewAnalytics(Long hostId) {
        log.debug("Fetching review analytics for host: {}", hostId);
        
        ReviewAnalyticsDTO analytics = new ReviewAnalyticsDTO();
        
        List<HostAnalytics> allAnalytics = hostAnalyticsRepository.findLatestAnalytics(hostId);
        
        if (!allAnalytics.isEmpty()) {
            HostAnalytics latest = allAnalytics.get(0);
            analytics.setAverageRating(latest.getAverageRating());
            analytics.setTotalReviews(latest.getTotalReviews());
            analytics.setFiveStarCount(latest.getFiveStarReviews());
            
            // Calculate star distribution (estimate)
            if (latest.getTotalReviews() > 0) {
                int fiveStarPercentage = (latest.getFiveStarReviews() * 100) / latest.getTotalReviews();
                analytics.setFiveStarCount(latest.getFiveStarReviews());
                analytics.setFourStarCount((int) (latest.getTotalReviews() * 0.15));
                analytics.setThreeStarCount((int) (latest.getTotalReviews() * 0.10));
                analytics.setTwoStarCount((int) (latest.getTotalReviews() * 0.03));
                analytics.setOneStarCount((int) (latest.getTotalReviews() * 0.02));
            }
        }
        
        return analytics;
    }
    
    // ========== Returning Guest Methods ==========
    
    /**
     * Get returning guests statistics
     */
    public List<ReturningGuestDTO> getReturningGuests(Long hostId, Integer limit) {
        log.debug("Fetching returning guests for host: {}", hostId);
        
        List<HostEarnings> returningGuestEarnings = hostEarningsRepository
            .findReturningGuestEarnings(hostId);
        
        Map<Long, List<HostEarnings>> guestBookings = returningGuestEarnings.stream()
            .collect(Collectors.groupingBy(HostEarnings::getGuestId));
        
        return guestBookings.entrySet().stream()
            .map(entry -> {
                Long guestId = entry.getKey();
                List<HostEarnings> bookings = entry.getValue();
                
                ReturningGuestDTO guest = new ReturningGuestDTO();
                guest.setGuestId(guestId);
                guest.setVisitCount(bookings.size());
                
                Double totalSpent = bookings.stream()
                    .map(b -> b.getNetEarnings().doubleValue())
                    .reduce(0.0, Double::sum);
                guest.setTotalSpent(totalSpent);
                
                if (!bookings.isEmpty()) {
                    guest.setLastVisitDate(bookings.get(0).getCheckOutDate().toString());
                }
                
                return guest;
            })
            .sorted(Comparator.comparingInt(ReturningGuestDTO::getVisitCount).reversed())
            .limit(limit != null ? limit : 10)
            .collect(Collectors.toList());
    }
    
    // ========== Earning Methods ==========
    
    /**
     * Get earnings history with pagination
     */
    public Page<HostEarningsDTO> getEarningsHistory(Long hostId, int page, int pageSize) {
        log.debug("Fetching earnings history for host: {} - page: {}, size: {}", hostId, page, pageSize);
        
        Page<HostEarnings> earnings = hostEarningsRepository.findByHostId(
            hostId, PageRequest.of(page, pageSize)
        );
        
        return earnings.map(e -> modelMapper.map(e, HostEarningsDTO.class));
    }
    
    /**
     * Record a new earning
     */
    public HostEarningsDTO recordEarning(Long hostId, HostEarningsDTO earningDTO) {
        log.debug("Recording new earning for host: {}", hostId);
        
        HostEarnings earning = modelMapper.map(earningDTO, HostEarnings.class);
        earning.setHostId(hostId);
        earning.setStatus("EARNED");
        
        earning = hostEarningsRepository.save(earning);
        return modelMapper.map(earning, HostEarningsDTO.class);
    }
    
    // ========== Helper Methods ==========
    
    private List<DailyMetricDTO> generateBookingsTrend(Long hostId, LocalDate start, LocalDate end) {
        List<HostAnalytics> analytics = hostAnalyticsRepository
            .findByHostIdAndDateRange(hostId, start, end);
        
        return analytics.stream()
            .map(a -> new DailyMetricDTO(
                a.getAnalyticsDate(),
                BigDecimal.valueOf(a.getTotalBookings()),
                "bookings"
            ))
            .sorted(Comparator.comparing(DailyMetricDTO::getDate))
            .collect(Collectors.toList());
    }
    
    private List<DailyMetricDTO> generateRevenueTrend(Long hostId, LocalDate start, LocalDate end) {
        List<HostEarnings> earnings = hostEarningsRepository
            .findByHostIdAndDateRange(hostId, start, end);
        
        Map<LocalDate, BigDecimal> dailyRevenue = new TreeMap<>();
        for (HostEarnings earning : earnings) {
            LocalDate date = earning.getCheckInDate();
            dailyRevenue.put(date, 
                dailyRevenue.getOrDefault(date, BigDecimal.ZERO).add(earning.getNetEarnings())
            );
        }
        
        return dailyRevenue.entrySet().stream()
            .map(e -> new DailyMetricDTO(e.getKey(), e.getValue(), "revenue"))
            .collect(Collectors.toList());
    }
    
    private List<DailyMetricDTO> generateOccupancyTrend(Long hostId, LocalDate start, LocalDate end) {
        List<HostAnalytics> analytics = hostAnalyticsRepository
            .findByHostIdAndDateRange(hostId, start, end);
        
        return analytics.stream()
            .map(a -> new DailyMetricDTO(
                a.getAnalyticsDate(),
                a.getOccupancyRate(),
                "occupancy"
            ))
            .sorted(Comparator.comparing(DailyMetricDTO::getDate))
            .collect(Collectors.toList());
    }
}
