package com.travolish.traveller.pricing.service.impl;

import com.travolish.traveller.pricing.dto.PricingSuggestionDTO;
import com.travolish.traveller.pricing.dto.PricingSuggestionRequest;
import com.travolish.traveller.pricing.entity.PricingSuggestion;
import com.travolish.traveller.pricing.repository.PricingSuggestionRepository;
import com.travolish.traveller.pricing.service.PricingAIService;
import com.travolish.traveller.booking.repository.BookingRepository;
import com.travolish.traveller.hotel.model.Room;
import com.travolish.traveller.hotel.repository.RoomRepository;
import com.travolish.traveller.inventory.repository.RoomAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PricingAIServiceImpl implements PricingAIService {

    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final RoomAvailabilityRepository availabilityRepository;

    @Override
    public PricingSuggestionDTO generateSuggestion(PricingSuggestionRequest request) {
        log.info("Generating pricing suggestion for room: {}", request.getRoomId());

        // Simulate AI analysis
        BigDecimal suggestedPrice = calculateAISuggestedPrice(request.getRoomId());
        BigDecimal currentPrice = BigDecimal.ZERO; // Would fetch from DB
        BigDecimal priceChange = suggestedPrice.subtract(currentPrice);
        
        double confidenceScore = Math.random();
        PricingSuggestion.SuggestionReason reason = determineSuggestionReason(request.getRoomId());
        PricingSuggestion.PricingTrend trend = determinePricingTrend(priceChange);

        PricingSuggestion suggestion = PricingSuggestion.builder()
                .hotelId(request.getHotelId())
                .roomId(request.getRoomId())
                .suggestedPrice(suggestedPrice)
                .currentPrice(currentPrice)
                .priceChange(priceChange)
                .confidenceScore(confidenceScore)
                .reason(reason)
                .trend(trend)
                .suggestedFromDate(request.getFromDate())
                .suggestedToDate(request.getToDate())
                .analysis("AI-generated pricing analysis based on demand trends and market conditions")
                .status(PricingSuggestion.SuggestionStatus.PENDING)
                .occupancyRate(request.getTargetOccupancyRate())
                .build();

        PricingSuggestion saved = pricingSuggestionRepository.save(suggestion);
        log.info("Pricing suggestion generated with ID: {} and confidence: {}", saved.getId(), confidenceScore);

        return mapToDTO(saved);
    }

    @Override
    public List<PricingSuggestionDTO> generateSuggestionsForHotel(Long hotelId) {
        List<Room> rooms = roomRepository.findByHotelId(hotelId);
        if (rooms.isEmpty()) return List.of();
        LocalDate today = LocalDate.now();
        return rooms.stream().map(room -> {
            PricingSuggestionRequest req = PricingSuggestionRequest.builder()
                    .hotelId(hotelId)
                    .roomId(room.getId())
                    .fromDate(today)
                    .toDate(today.plusDays(30))
                    .build();
            return generateSuggestion(req);
        }).collect(Collectors.toList());
    }

    @Override
    public List<PricingSuggestionDTO> getSuggestionsForHotel(Long hotelId) {
        return pricingSuggestionRepository.findByHotelId(hotelId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PricingSuggestionDTO> getSuggestionsForRoom(Long roomId) {
        return pricingSuggestionRepository.findByRoomId(roomId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PricingSuggestionDTO> getPendingSuggestionsForHotel(Long hotelId, Pageable pageable) {
        return pricingSuggestionRepository.findByHotelIdAndStatus(hotelId, PricingSuggestion.SuggestionStatus.PENDING, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public PricingSuggestionDTO acceptSuggestion(Long suggestionId) {
        PricingSuggestion suggestion = pricingSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));
        
        suggestion.setStatus(PricingSuggestion.SuggestionStatus.ACCEPTED);
        suggestion.setAcceptedAt(LocalDateTime.now());
        
        PricingSuggestion saved = pricingSuggestionRepository.save(suggestion);
        log.info("Pricing suggestion {} accepted", suggestionId);
        
        return mapToDTO(saved);
    }

    @Override
    public PricingSuggestionDTO rejectSuggestion(Long suggestionId, String reason) {
        PricingSuggestion suggestion = pricingSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> new RuntimeException("Suggestion not found"));
        
        suggestion.setStatus(PricingSuggestion.SuggestionStatus.REJECTED);
        suggestion.setRejectedAt(LocalDateTime.now());
        suggestion.setRejectionReason(reason);
        
        PricingSuggestion saved = pricingSuggestionRepository.save(suggestion);
        log.info("Pricing suggestion {} rejected: {}", suggestionId, reason);
        
        return mapToDTO(saved);
    }

    @Override
    public List<PricingSuggestionDTO> analyzeDemandTrends(Long hotelId) {
        log.info("Analyzing demand trends for hotel: {}", hotelId);
        // Would implement actual demand analysis logic
        return getSuggestionsForHotel(hotelId);
    }

    @Override
    public List<PricingSuggestionDTO> analyzeCompetitorPricing(Long hotelId) {
        log.info("Analyzing competitor pricing for hotel: {}", hotelId);
        // Would implement actual competitor analysis logic
        return getSuggestionsForHotel(hotelId);
    }

    @Override
    public List<PricingSuggestionDTO> generateSeasonalPricingSuggestions(Long hotelId) {
        log.info("Generating seasonal pricing suggestions for hotel: {}", hotelId);
        // Would implement seasonal pricing logic
        return getSuggestionsForHotel(hotelId);
    }

    /**
     * Suggests a price using real booking history + occupancy data.
     *
     * Logic:
     *  1. Get current base price from the room record.
     *  2. Compute 30-day average occupancy for the room.
     *  3. Apply demand-based multiplier:
     *       > 80% occupancy → +20%  (high demand)
     *       60–80%          → +10%
     *       40–60%          → no change
     *       20–40%          → -10%  (low demand)
     *       < 20%           → -20%
     *  4. Apply seasonal boost if it's within a known peak period.
     */
    private BigDecimal calculateAISuggestedPrice(Long roomId) {
        // Get room's base price
        BigDecimal basePrice = roomRepository.findById(roomId)
            .map(r -> BigDecimal.valueOf(r.getPricePerNight() != null ? r.getPricePerNight() : 0.0))
            .orElse(BigDecimal.valueOf(100));

        if (basePrice.compareTo(BigDecimal.ZERO) == 0) {
            basePrice = BigDecimal.valueOf(100); // fallback
        }

        // 30-day average occupancy from availability records
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);
        OptionalDouble avgOccupancy = availabilityRepository
            .findByRoomIdAndAvailabilityDateBetween(roomId, start, end)
            .stream()
            .mapToDouble(a -> a.getOccupancyPercentage() != null ? a.getOccupancyPercentage() : 0.0)
            .average();

        double occupancy = avgOccupancy.orElse(50.0);

        // Demand multiplier
        double multiplier;
        if (occupancy > 80) multiplier = 1.20;
        else if (occupancy > 60) multiplier = 1.10;
        else if (occupancy > 40) multiplier = 1.00;
        else if (occupancy > 20) multiplier = 0.90;
        else multiplier = 0.80;

        // Seasonal boost for peak months (Dec–Jan, May–Jun in India)
        int month = LocalDate.now().getMonthValue();
        if (month == 12 || month == 1 || month == 5 || month == 6) multiplier += 0.05;

        return basePrice.multiply(BigDecimal.valueOf(multiplier)).setScale(0, RoundingMode.HALF_UP);
    }

    private PricingSuggestion.SuggestionReason determineSuggestionReason(Long roomId) {
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(30);
        OptionalDouble avg = availabilityRepository
            .findByRoomIdAndAvailabilityDateBetween(roomId, start, end)
            .stream()
            .mapToDouble(a -> a.getOccupancyPercentage() != null ? a.getOccupancyPercentage() : 0.0)
            .average();
        double occ = avg.orElse(50.0);
        if (occ > 75) return PricingSuggestion.SuggestionReason.HIGH_DEMAND;
        if (occ < 30) return PricingSuggestion.SuggestionReason.LOW_OCCUPANCY;
        int month = LocalDate.now().getMonthValue();
        if (month == 12 || month == 1 || month == 5 || month == 6) return PricingSuggestion.SuggestionReason.SEASONAL_TREND;
        return PricingSuggestion.SuggestionReason.MARKET_ADJUSTMENT;
    }

    private PricingSuggestion.PricingTrend determinePricingTrend(BigDecimal priceChange) {
        if (priceChange.signum() > 0) return PricingSuggestion.PricingTrend.INCREASE;
        if (priceChange.signum() < 0) return PricingSuggestion.PricingTrend.DECREASE;
        return PricingSuggestion.PricingTrend.STABLE;
    }

    private PricingSuggestionDTO mapToDTO(PricingSuggestion suggestion) {
        return PricingSuggestionDTO.builder()
                .id(suggestion.getId())
                .hotelId(suggestion.getHotelId())
                .roomId(suggestion.getRoomId())
                .suggestedPrice(suggestion.getSuggestedPrice())
                .currentPrice(suggestion.getCurrentPrice())
                .priceChange(suggestion.getPriceChange())
                .confidenceScore(suggestion.getConfidenceScore())
                .reason(suggestion.getReason().toString())
                .trend(suggestion.getTrend().toString())
                .suggestedFromDate(suggestion.getSuggestedFromDate())
                .suggestedToDate(suggestion.getSuggestedToDate())
                .analysis(suggestion.getAnalysis())
                .status(suggestion.getStatus().toString())
                .occupancyRate(suggestion.getOccupancyRate())
                .competitorAvgPrice(suggestion.getCompetitorAvgPrice())
                .demandLevel(suggestion.getDemandLevel())
                .createdAt(suggestion.getCreatedAt())
                .build();
    }
}
