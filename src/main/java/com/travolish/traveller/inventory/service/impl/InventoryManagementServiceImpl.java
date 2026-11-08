package com.travolish.traveller.inventory.service.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.travolish.traveller.inventory.service.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.travolish.traveller.inventory.dto.AvailabilityCheckDTO;
import com.travolish.traveller.inventory.dto.InventoryAlertDTO;
import com.travolish.traveller.inventory.dto.InventoryDashboardDTO;
import com.travolish.traveller.inventory.dto.InventoryForecastDTO;
import com.travolish.traveller.inventory.dto.OccupancyReportDTO;
import com.travolish.traveller.inventory.dto.PricingCalculationDTO;
import com.travolish.traveller.inventory.dto.PricingRecommendationDTO;
import com.travolish.traveller.inventory.dto.PricingReportDTO;
import com.travolish.traveller.inventory.dto.RevenueForecastDTO;
import com.travolish.traveller.inventory.dto.RoomInventoryInitDTO;
import com.travolish.traveller.inventory.exception.InsufficientAvailabilityException;
import com.travolish.traveller.inventory.model.RoomAvailability;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.inventory.repository.PricingRuleRepository;
import com.travolish.traveller.inventory.repository.RoomAvailabilityRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryManagementServiceImpl implements InventoryManagementService {

    private final AvailabilityService availabilityService;
    private final SeasonalPricingService seasonalPricingService;
    private final DynamicPricingService dynamicPricingService;
    private final RoomAvailabilityRepository availabilityRepository;
    private final PricingRuleRepository pricingRuleRepository;
    private final BookingRepository bookingRepository;

    @Override
    public Boolean canBookRoom(Long roomId, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
        try {
            return availabilityService.isRoomAvailableForDateRange(roomId, checkInDate, checkOutDate);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PricingCalculationDTO calculateBookingPrice(
        Long roomId, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate, Double basePrice) {
        
        Integer numberOfNights = (int) ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        
        PricingCalculationDTO calculation = PricingCalculationDTO.builder()
            .roomId(roomId)
            .hotelId(hotelId)
            .checkInDate(checkInDate)
            .checkOutDate(checkOutDate)
            .basePrice(basePrice)
            .numberOfNights(numberOfNights)
            .seasonalAdjustment(0.0)
            .dynamicAdjustment(0.0)
            .promotionalDiscount(0.0)
            .build();

        double priceWithSeasonal = seasonalPricingService.calculatePriceForDateRange(
            roomId, checkInDate, checkOutDate, basePrice
        );
        calculation.setSeasonalAdjustment(priceWithSeasonal - (basePrice * numberOfNights));

        // Average dynamic adjustment across dates
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = checkInDate;
        while (current.isBefore(checkOutDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }

        double totalDynamicAdjustment = 0;
        for (LocalDate date : dates) {
            Double dynamicMultiplier = dynamicPricingService.getPriceMultiplier(roomId, date);
            double dayPrice = basePrice * dynamicMultiplier;
            totalDynamicAdjustment += (dayPrice - basePrice);
        }
        calculation.setDynamicAdjustment(totalDynamicAdjustment);

        double finalPricePerNight = (priceWithSeasonal / numberOfNights) + (totalDynamicAdjustment / numberOfNights);
        calculation.setFinalPrice(finalPricePerNight);
        calculation.setTotalPrice(finalPricePerNight * numberOfNights);

        calculation.setPriceBreakdown(calculation.getDetailedBreakdown());

        return calculation;
    }

    @Override
    @Transactional
    public void processBooking(Long roomId, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (!canBookRoom(roomId, hotelId, checkInDate, checkOutDate)) {
            throw InsufficientAvailabilityException.allDatesFullyBooked(roomId);
        }

        availabilityService.bookRoom(roomId, hotelId, checkInDate, checkOutDate);
    }

    @Override
    @Transactional
    public void processCancellation(Long roomId, LocalDate checkInDate, LocalDate checkOutDate) {
        availabilityService.cancelBooking(roomId, checkInDate, checkOutDate);
    }

    @Override
    public InventoryDashboardDTO getInventoryDashboard(Long hotelId, LocalDate date) {
        AvailabilityCheckDTO occupancy = availabilityService.getHotelOccupancyOnDate(hotelId, date);

        List<AvailabilityCheckDTO> roomDetails = availabilityService
            .getHotelOccupancyForDateRange(hotelId, date, date.plusDays(1));

        Double occPct = occupancy.getOccupancyPercentage();
        String status;
        if (occPct == null || occPct == 0.0) {
            status = "VACANT";
        } else if (occPct < 20) {
            status = "LOW";
        } else if (occPct > 85) {
            status = "CRITICAL";
        } else if (occPct > 70) {
            status = "WARNING";
        } else {
            status = "GOOD";
        }

        return InventoryDashboardDTO.builder()
            .hotelId(hotelId)
            .date(date)
            .totalRooms(occupancy.getTotalRooms())
            .bookedRooms(occupancy.getBookedRooms())
            .availableRooms(occupancy.getAvailableRooms())
            .occupancyPercentage(occupancy.getOccupancyPercentage())
            .status(status)
            .roomDetails(roomDetails)
            .build();
    }

    @Override
    public List<InventoryForecastDTO> getInventoryForecast(Long hotelId, LocalDate startDate, LocalDate endDate) {
        List<InventoryForecastDTO> forecasts = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            var occupancy = availabilityService.getHotelOccupancyOnDate(hotelId, current);
            
            InventoryForecastDTO forecast = InventoryForecastDTO.builder()
                .date(current)
                .projectedOccupancy(occupancy.getOccupancyPercentage() != null
                    ? (int) Math.round(occupancy.getOccupancyPercentage()) : 0)
                .demandLevel(getDemandLevelLabel(occupancy.getOccupancyPercentage()))
                .build();

            forecasts.add(forecast);
            current = current.plusDays(1);
        }

        return forecasts;
    }

    @Override
    public PricingReportDTO generatePricingReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        var rules = pricingRuleRepository.findByHotelIdAndIsActiveTrue(hotelId);
        
        double avgPrice = 0.0;
        double minPrice = Double.MAX_VALUE;
        double maxPrice = 0.0;
        int count = 0;

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            LocalDate finalCurrent = current;
            for (var rule : rules) {
                if (rule.appliesToDate(finalCurrent)) {
                    Double price = rule.calculatePrice();
                    avgPrice = (avgPrice * count + price) / (count + 1);
                    minPrice = Math.min(minPrice, price);
                    maxPrice = Math.max(maxPrice, price);
                    count++;
                }
            }
            current = current.plusDays(1);
        }

        Double estimatedRevenue = calculateEstimatedRevenue(hotelId, startDate, endDate, avgPrice);

        return PricingReportDTO.builder()
            .hotelId(hotelId)
            .startDate(startDate)
            .endDate(endDate)
            .averagePrice(avgPrice)
            .minPrice(minPrice == Double.MAX_VALUE ? 0.0 : minPrice)
            .maxPrice(maxPrice)
            .rulesApplied(rules.size())
            .estimatedRevenue(estimatedRevenue)
            .build();
    }

    @Override
    public OccupancyReportDTO generateOccupancyReport(Long hotelId, LocalDate startDate, LocalDate endDate) {
        List<RoomAvailability> availabilities = availabilityRepository
            .findByHotelIdAndAvailabilityDateBetween(hotelId, startDate, endDate);

        double totalOccupancy = 0;
        double peakOccupancy = 0;
        double lowOccupancy = 100;
        int totalBookings = 0;
        int totalCancellations = 0;

        for (RoomAvailability availability : availabilities) {
            double occupancy = availability.getOccupancyPercentage();
            totalOccupancy += occupancy;
            peakOccupancy = Math.max(peakOccupancy, occupancy);
            lowOccupancy = Math.min(lowOccupancy, occupancy);
            totalBookings += availability.getBookedRooms();
        }

        double avgOccupancy = availabilities.isEmpty() ? 0 : totalOccupancy / availabilities.size();

        // Load all bookings in the period to compute cancellation rate and segment breakdown
        List<com.travolish.traveller.booking.model.Booking> allPeriodBookings =
            bookingRepository.findByHotelId(hotelId).stream()
                .filter(b -> !b.getCheckInDate().isBefore(startDate)
                          && !b.getCheckInDate().isAfter(endDate))
                .collect(java.util.stream.Collectors.toList());

        totalCancellations = (int) allPeriodBookings.stream()
            .filter(b -> b.getStatus() == com.travolish.traveller.booking.model.Booking.BookingStatus.CANCELLED)
            .count();

        // Confirmed bookings only — used for length-of-stay segment breakdown
        List<com.travolish.traveller.booking.model.Booking> periodBookings = allPeriodBookings.stream()
            .filter(b -> b.getStatus() == com.travolish.traveller.booking.model.Booking.BookingStatus.CONFIRMED)
            .collect(java.util.stream.Collectors.toList());

        int periodReservations = periodBookings.size() + totalCancellations;
        double cancellationRate = periodReservations == 0 ? 0 : (totalCancellations * 100.0 / periodReservations);

        List<OccupancyReportDTO.SegmentDTO> segments = new ArrayList<>();
        if (!periodBookings.isEmpty()) {
            long shortStays  = periodBookings.stream().filter(b -> ChronoUnit.DAYS.between(b.getCheckInDate(), b.getCheckOutDate()) <= 2).count();
            long mediumStays = periodBookings.stream().filter(b -> { long n = ChronoUnit.DAYS.between(b.getCheckInDate(), b.getCheckOutDate()); return n >= 3 && n <= 6; }).count();
            long longStays   = periodBookings.stream().filter(b -> ChronoUnit.DAYS.between(b.getCheckInDate(), b.getCheckOutDate()) >= 7).count();
            long total = periodBookings.size();

            segments.add(OccupancyReportDTO.SegmentDTO.builder()
                .segmentName("Short stays (1–2 nights)")
                .percentage(Math.round(shortStays * 1000.0 / total) / 10.0)
                .trend(shortStays > 0 ? "Active" : "Low")
                .build());
            segments.add(OccupancyReportDTO.SegmentDTO.builder()
                .segmentName("Medium stays (3–6 nights)")
                .percentage(Math.round(mediumStays * 1000.0 / total) / 10.0)
                .trend(mediumStays > 0 ? "Active" : "Low")
                .build());
            segments.add(OccupancyReportDTO.SegmentDTO.builder()
                .segmentName("Long stays (7+ nights)")
                .percentage(Math.round(longStays * 1000.0 / total) / 10.0)
                .trend(longStays > 0 ? "Active" : "Low")
                .build());
        }

        return OccupancyReportDTO.builder()
            .hotelId(hotelId)
            .startDate(startDate)
            .endDate(endDate)
            .averageOccupancy(avgOccupancy)
            .peakOccupancy(peakOccupancy)
            .lowOccupancy(lowOccupancy)
            .totalBookings(totalBookings)
            .totalCancellations(totalCancellations)
            .cancellationRate(cancellationRate)
            .segments(segments)
            .build();
    }

    @Override
    public RevenueForecastDTO generateRevenueForecast(Long hotelId, LocalDate startDate, LocalDate endDate) {
        List<InventoryForecastDTO> dailyForecasts = new ArrayList<>();
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            Double dayRevenue = bookingRepository.getTotalRevenueForHotelInPeriod(
                hotelId, current, current.plusDays(1));
            var occupancy = availabilityService.getHotelOccupancyOnDate(hotelId, current);
            var forecast = new InventoryForecastDTO();
            forecast.setDate(current);
            forecast.setProjectedOccupancy(occupancy.getOccupancyPercentage() != null
                ? (int) Math.round(occupancy.getOccupancyPercentage()) : 0);
            forecast.setProjectedRevenue(dayRevenue);                             // daily revenue in correct field
            forecast.setDemandLevel(getDemandLevelLabel(occupancy.getOccupancyPercentage()));
            dailyForecasts.add(forecast);
            current = current.plusDays(1);
        }

        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double[] curr = computePeriodMetrics(hotelId, startDate, endDate);
        double avgDailyRevenue = days > 0 ? curr[0] / days : 0.0;

        LocalDate priorStart = startDate.minusDays(days);
        LocalDate priorEnd = startDate.minusDays(1);
        double[] prior = computePeriodMetrics(hotelId, priorStart, priorEnd);

        Double revenueGrowth = pctChange(prior[0], curr[0]);
        Double adrChange = pctChange(prior[1], curr[1]);
        Double revParChange = pctChange(prior[2], curr[2]);

        return RevenueForecastDTO.builder()
            .hotelId(hotelId)
            .startDate(startDate)
            .endDate(endDate)
            .totalRevenue(curr[0])
            .averageDailyRevenue(avgDailyRevenue)
            .estimatedRevenue(curr[2])
            .revenueTrend(revenueGrowth)
            .revenueGrowth(revenueGrowth)
            .adrChange(adrChange)
            .revParChange(revParChange)
            .dailyForecasts(dailyForecasts)
            .build();
    }

    // Returns [revenue, adr, revpar] for the given period.
    // ADR  = revenue / total room-nights sold (bookedRooms across all availability records)
    // RevPAR = revenue / total room-nights available (totalRooms across all availability records)
    private double[] computePeriodMetrics(Long hotelId, LocalDate start, LocalDate end) {
        Double revenue = bookingRepository.getTotalRevenueForHotelInPeriod(hotelId, start, end.plusDays(1));
        double rev = revenue != null ? revenue : 0.0;

        List<RoomAvailability> records = availabilityRepository
            .findByHotelIdAndAvailabilityDateBetween(hotelId, start, end);

        long bookedNights = records.stream().mapToLong(RoomAvailability::getBookedRooms).sum();
        long totalNights  = records.stream().mapToLong(RoomAvailability::getTotalRooms).sum();

        double adr    = bookedNights > 0 ? rev / bookedNights : 0.0;
        double revpar = totalNights  > 0 ? rev / totalNights  : 0.0;

        return new double[]{rev, adr, revpar};
    }

    // Returns null when prior is zero to avoid misleading infinity/division-by-zero results.
    private Double pctChange(double prior, double current) {
        if (prior == 0.0) return null;
        return (current - prior) / prior * 100.0;
    }

    @Override
    public List<PricingRecommendationDTO> getPricingRecommendations(
        Long hotelId, LocalDate startDate, LocalDate endDate) {
        
        List<PricingRecommendationDTO> recommendations = new ArrayList<>();

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            var occupancy = availabilityService.getHotelOccupancyOnDate(hotelId, current);
            
            LocalDate finalCurrent = current;
            double occupancyRate = occupancy.getOccupancyPercentage();

            if (occupancyRate > 85) {
                recommendations.add(PricingRecommendationDTO.builder()
                    .date(finalCurrent)
                    .recommendedPrice(100.0) // Placeholder
                    .reason("HIGH_DEMAND")
                    .estimatedImpact(15.0)
                    .build());
            } else if (occupancyRate < 30) {
                recommendations.add(PricingRecommendationDTO.builder()
                    .date(finalCurrent)
                    .recommendedPrice(70.0) // Placeholder
                    .reason("LOW_DEMAND")
                    .estimatedImpact(-20.0)
                    .build());
            }

            current = current.plusDays(1);
        }

        return recommendations;
    }

    @Override
    public List<InventoryAlertDTO> getAvailabilityAlerts(Long hotelId) {
        List<InventoryAlertDTO> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        // Check for low stock
        List<AvailabilityCheckDTO> limitedRooms = availabilityService
            .getRoomsWithLimitedAvailability(hotelId, today);

        limitedRooms.forEach(room -> alerts.add(InventoryAlertDTO.builder()
            .roomId(room.getRoomId())
            .date(today)
            .alertType("LOW_STOCK")
            .message("Limited availability: " + room.getAvailableRooms() + " rooms remaining")
            .severity("WARNING")
            .build()));

        return alerts;
    }

    @Override
    @Transactional
    public void adjustAvailability(Long roomId, LocalDate date, Integer quantityChange, String reason) {
        var availability = availabilityRepository.findByRoomIdAndAvailabilityDate(roomId, date);

        if (availability.isPresent()) {
            RoomAvailability ra = availability.get();
            if (quantityChange > 0) {
                ra.removeBooking();
            } else if (quantityChange < 0) {
                ra.addBooking();
            }
            availabilityRepository.save(ra);
        }
    }

    @Override
    @Transactional
    public void bulkUpdateAvailability(
        Long hotelId, LocalDate startDate, LocalDate endDate, Integer adjustment, String reason) {
        
        List<RoomAvailability> availabilities = availabilityRepository
            .findByHotelIdAndAvailabilityDateBetween(hotelId, startDate, endDate);

        availabilities.forEach(availability -> {
            if (adjustment > 0) {
                for (int i = 0; i < adjustment; i++) {
                    availability.removeBooking();
                }
            } else if (adjustment < 0) {
                for (int i = 0; i < Math.abs(adjustment); i++) {
                    availability.addBooking();
                }
            }
            availabilityRepository.save(availability);
        });
    }

    @Override
    @Transactional
    public void initializeHotelInventory(
        Long hotelId, List<RoomInventoryInitDTO> rooms, Integer daysAhead) {
        
        rooms.forEach(room -> availabilityService.initializeRoomAvailability(
            hotelId, room.getRoomId(), room.getRoomCount(), daysAhead
        ));
    }

    /**
     * Helper method to determine demand level label
     */
    private String getDemandLevelLabel(Double occupancyPercentage) {
        if (occupancyPercentage == null) return "LOW";
        if (occupancyPercentage < 30) return "LOW";
        if (occupancyPercentage < 60) return "MEDIUM";
        if (occupancyPercentage < 85) return "HIGH";
        return "VERY_HIGH";
    }

    /**
     * Helper method to calculate estimated revenue
     */
    private Double calculateEstimatedRevenue(Long hotelId, LocalDate startDate, LocalDate endDate, Double avgPrice) {
        var occupancies = availabilityRepository.findByHotelIdAndAvailabilityDateBetween(hotelId, startDate, endDate);
        double totalBookings = occupancies.stream().mapToInt(RoomAvailability::getBookedRooms).sum();
        return totalBookings * avgPrice;
    }
}
