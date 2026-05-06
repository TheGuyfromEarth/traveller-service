package com.travolish.traveller.pricing.service.impl;

import com.travolish.traveller.pricing.dto.PricingSuggestionDTO;
import com.travolish.traveller.pricing.dto.PricingSuggestionRequest;
import com.travolish.traveller.pricing.entity.PricingSuggestion;
import com.travolish.traveller.pricing.repository.PricingSuggestionRepository;
import com.travolish.traveller.pricing.service.PricingAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PricingAIServiceImpl implements PricingAIService {

    private final PricingSuggestionRepository pricingSuggestionRepository;

    @Override
    public PricingSuggestionDTO generateSuggestion(PricingSuggestionRequest request) {
        log.info("Generating pricing suggestion for room: {}", request.getRoomId());

        // Simulate AI analysis
        BigDecimal suggestedPrice = calculateAISuggestedPrice(request.getRoomId());
        BigDecimal currentPrice = BigDecimal.ZERO; // Would fetch from DB
        BigDecimal priceChange = suggestedPrice.subtract(currentPrice);
        
        double confidenceScore = Math.random();
        PricingSuggestion.SuggestionReason reason = determineSuggestionReason();
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
        return pricingSuggestionRepository.findByHotelIdAndStatus(hotelId, "PENDING", pageable)
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

    private BigDecimal calculateAISuggestedPrice(Long roomId) {
        // Simulate AI calculation
        return BigDecimal.valueOf(Math.random() * 500 + 50);
    }

    private PricingSuggestion.SuggestionReason determineSuggestionReason() {
        PricingSuggestion.SuggestionReason[] reasons = PricingSuggestion.SuggestionReason.values();
        return reasons[(int) (Math.random() * reasons.length)];
    }

    private PricingSuggestion.PricingTrend determinePricingTrend(BigDecimal priceChange) {
        if (priceChange.signum() > 0) {
            return PricingSuggestion.PricingTrend.INCREASE;
        } else if (priceChange.signum() < 0) {
            return PricingSuggestion.PricingTrend.DECREASE;
        }
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
