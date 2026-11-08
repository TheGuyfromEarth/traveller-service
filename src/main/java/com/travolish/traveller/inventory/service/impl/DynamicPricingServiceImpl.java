package com.travolish.traveller.inventory.service.impl;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travolish.traveller.inventory.dto.DemandMetricsDTO;
import com.travolish.traveller.inventory.model.DemandMetrics;
import com.travolish.traveller.inventory.model.DemandMetrics.DemandLevel;
import com.travolish.traveller.inventory.repository.DemandMetricsRepository;
import com.travolish.traveller.inventory.service.DynamicPricingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DynamicPricingServiceImpl implements DynamicPricingService {

    private final DemandMetricsRepository demandMetricsRepository;

    @Override
    public Double calculateDynamicPrice(Long roomId, LocalDate date) {
        var metrics = demandMetricsRepository.findByRoomIdAndMetricDate(roomId, date);
        if (metrics.isEmpty()) {
            return 1.0; // Default multiplier if no metrics
        }
        return metrics.get().getPriceMultiplier();
    }

    @Override
    public Double calculatePriceForDateRange(
        Long roomId, LocalDate checkInDate, LocalDate checkOutDate, Double basePrice) {
        
        List<DemandMetrics> allMetrics = demandMetricsRepository
            .findByRoomIdAndMetricDateBetween(roomId, checkInDate, checkOutDate.minusDays(1));

        if (allMetrics.isEmpty()) {
            long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            return basePrice * nights;
        }

        double totalPrice = 0;
        for (DemandMetrics metric : allMetrics) {
            totalPrice += basePrice * metric.getPriceMultiplier();
        }
        return totalPrice;
    }

    @Override
    public DemandLevel getDemandLevel(Long roomId, LocalDate date) {
        return demandMetricsRepository.findByRoomIdAndMetricDate(roomId, date)
            .map(DemandMetrics::getDemandLevel)
            .orElse(DemandLevel.MEDIUM);
    }

    @Override
    public Double getPriceMultiplier(Long roomId, LocalDate date) {
        return demandMetricsRepository.findByRoomIdAndMetricDate(roomId, date)
            .map(DemandMetrics::getPriceMultiplier)
            .orElse(1.0);
    }

    @Override
    public Double calculatePriceByOccupancy(Double basePrice, Double occupancyPercentage) {
        if (occupancyPercentage < 30) {
            return basePrice * 0.7; // 30% discount
        } else if (occupancyPercentage < 60) {
            return basePrice * 1.0; // Base price
        } else if (occupancyPercentage < 85) {
            return basePrice * 1.3; // 30% premium
        } else {
            return basePrice * 1.6; // 60% premium
        }
    }

    @Override
    public Double calculatePriceByVelocity(Double basePrice, Double bookingVelocity, Integer daysUntilDate) {
        // High velocity close to date = higher price
        double velocityFactor = 1.0 + (bookingVelocity * 0.1); // 10% per booking velocity
        double timingFactor = Math.max(0.5, 1.0 - (daysUntilDate / 180.0)); // Closer to date = higher price
        
        return basePrice * velocityFactor * (1.0 + timingFactor);
    }

    @Override
    @Transactional
    public DemandMetricsDTO recordDemandMetrics(DemandMetricsDTO metricsDTO) {
        DemandMetrics metrics = DemandMetrics.builder()
            .roomId(metricsDTO.getRoomId())
            .hotelId(metricsDTO.getHotelId())
            .metricDate(metricsDTO.getMetricDate())
            .occupancyRate(metricsDTO.getOccupancyRate())
            .bookingsCount(metricsDTO.getBookingsCount())
            .cancelledCount(metricsDTO.getCancelledCount())
            .viewCount(metricsDTO.getViewCount())
            .inquiryCount(metricsDTO.getInquiryCount())
            .averageBookingValue(metricsDTO.getAverageBookingValue())
            .daysSinceLastBooking(metricsDTO.getDaysSinceLastBooking())
            .notes(metricsDTO.getNotes())
            .build();

        metrics.calculateDemandLevel();
        DemandMetrics saved = demandMetricsRepository.save(metrics);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public void updateDemandMetrics(Long roomId, LocalDate date) {
        var existing = demandMetricsRepository.findByRoomIdAndMetricDate(roomId, date)
            .orElseGet(() -> DemandMetrics.builder()
                .roomId(roomId)
                .metricDate(date)
                .occupancyRate(0.0)
                .bookingsCount(0)
                .cancelledCount(0)
                .viewCount(0)
                .build());

        existing.calculateDemandLevel();
        existing.setUpdatedAt(OffsetDateTime.now());
        demandMetricsRepository.save(existing);
    }

    @Override
    public List<DemandMetricsDTO> getDemandMetricsForDateRange(
        Long roomId, LocalDate startDate, LocalDate endDate) {
        
        return demandMetricsRepository.findByRoomIdAndMetricDateBetween(roomId, startDate, endDate)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<DemandMetricsDTO> findHighDemandPeriods(Long roomId, LocalDate startDate, LocalDate endDate) {
        return demandMetricsRepository.findHighDemandDates(roomId, startDate, endDate)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public List<DemandMetricsDTO> findLowDemandPeriods(Long roomId, LocalDate startDate, LocalDate endDate) {
        return demandMetricsRepository.findLowDemandDates(roomId, startDate, endDate)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    @Override
    public Double getAverageOccupancy(Long roomId, LocalDate startDate, LocalDate endDate) {
        return demandMetricsRepository.getAverageOccupancyRate(roomId, startDate, endDate).orElse(0.0);
    }

    @Override
    public Double getCancellationRate(Long roomId, LocalDate startDate, LocalDate endDate) {
        return demandMetricsRepository.getCancellationRateForRoom(roomId, startDate, endDate);
    }

    @Override
    public Double getConversionRate(Long roomId, LocalDate startDate, LocalDate endDate) {
        return demandMetricsRepository.getConversionRate(roomId, startDate, endDate);
    }

    @Override
    public Double predictPriceForDate(Long roomId, LocalDate date) {
        // Simple prediction based on historical demand
        LocalDate lookbackStart = date.minusMonths(1);
        Double avgOccupancy = getAverageOccupancy(roomId, lookbackStart, date);
        Double predictedMultiplier = calculateOccupancyMultiplier(avgOccupancy);
        return predictedMultiplier;
    }

    @Override
    public String getTrendingDemandDirection(Long roomId, LocalDate startDate, LocalDate endDate) {
        LocalDate midDate = startDate.plusDays((endDate.toEpochDay() - startDate.toEpochDay()) / 2);
        
        Double firstHalfOccupancy = getAverageOccupancy(roomId, startDate, midDate);
        Double secondHalfOccupancy = getAverageOccupancy(roomId, midDate, endDate);

        if (secondHalfOccupancy > firstHalfOccupancy * 1.1) {
            return "INCREASING";
        } else if (firstHalfOccupancy > secondHalfOccupancy * 1.1) {
            return "DECREASING";
        } else {
            return "STABLE";
        }
    }

    @Override
    public Double calculateOptimalPrice(Long roomId, LocalDate date, Double basePrice) {
        var metrics = demandMetricsRepository.findByRoomIdAndMetricDate(roomId, date);
        if (metrics.isEmpty()) {
            return basePrice;
        }

        DemandMetrics metric = metrics.get();
        // Maximize revenue = price * (1 - (price/max_price)^elasticity)
        // For simplicity, use demand-based multiplier
        return basePrice * metric.getPriceMultiplier();
    }

    @Override
    @Transactional
    public Long archiveOldDemandMetrics(LocalDate beforeDate) {
        return demandMetricsRepository.deleteByMetricDateBefore(beforeDate);
    }

    /**
     * Helper to calculate occupancy-based multiplier
     */
    private Double calculateOccupancyMultiplier(Double occupancyRate) {
        if (occupancyRate < 30) return 0.7;
        if (occupancyRate < 60) return 1.0;
        if (occupancyRate < 85) return 1.3;
        return 1.6;
    }

    /**
     * Convert entity to DTO
     */
    private DemandMetricsDTO convertToDTO(DemandMetrics metrics) {
        return DemandMetricsDTO.builder()
            .id(metrics.getId())
            .roomId(metrics.getRoomId())
            .hotelId(metrics.getHotelId())
            .metricDate(metrics.getMetricDate())
            .occupancyRate(metrics.getOccupancyRate())
            .bookingsCount(metrics.getBookingsCount())
            .cancelledCount(metrics.getCancelledCount())
            .viewCount(metrics.getViewCount())
            .inquiryCount(metrics.getInquiryCount())
            .averageBookingValue(metrics.getAverageBookingValue())
            .daysSinceLastBooking(metrics.getDaysSinceLastBooking())
            .demandLevel(metrics.getDemandLevel().toString())
            .priceMultiplier(metrics.getPriceMultiplier())
            .notes(metrics.getNotes())
            .createdAt(metrics.getCreatedAt())
            .updatedAt(metrics.getUpdatedAt())
            .build();
    }
}
