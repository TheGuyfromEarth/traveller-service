package com.travolish.traveller.pricing.service;

import com.travolish.traveller.pricing.dto.CompetitorAnalysisDTO;
import com.travolish.traveller.pricing.dto.DemandAnalysisDTO;
import com.travolish.traveller.pricing.dto.PricingSuggestionDTO;
import com.travolish.traveller.pricing.dto.PricingSuggestionRequest;
import com.travolish.traveller.pricing.dto.SeasonalPricingDTO;
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

    DemandAnalysisDTO analyzeDemandTrends(Long hotelId);

    CompetitorAnalysisDTO analyzeCompetitorPricing(Long hotelId);

    SeasonalPricingDTO generateSeasonalPricingSuggestions(Long hotelId);
}
