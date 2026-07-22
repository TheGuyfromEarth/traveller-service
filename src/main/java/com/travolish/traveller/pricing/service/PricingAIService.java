package com.travolish.traveller.pricing.service;

import com.travolish.traveller.pricing.dto.PricingSuggestionDTO;
import com.travolish.traveller.pricing.dto.PricingSuggestionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface PricingAIService {
    PricingSuggestionDTO generateSuggestion(PricingSuggestionRequest request);

    List<PricingSuggestionDTO> generateSuggestionsForHotel(Long hotelId);
    
    List<PricingSuggestionDTO> getSuggestionsForHotel(Long hotelId);
    
    List<PricingSuggestionDTO> getSuggestionsForRoom(Long roomId);
    
    Page<PricingSuggestionDTO> getPendingSuggestionsForHotel(Long hotelId, Pageable pageable);
    
    PricingSuggestionDTO acceptSuggestion(Long suggestionId);
    
    PricingSuggestionDTO rejectSuggestion(Long suggestionId, String reason);
    
    List<PricingSuggestionDTO> analyzeDemandTrends(Long hotelId);
    
    List<PricingSuggestionDTO> analyzeCompetitorPricing(Long hotelId);
    
    List<PricingSuggestionDTO> generateSeasonalPricingSuggestions(Long hotelId);
}
