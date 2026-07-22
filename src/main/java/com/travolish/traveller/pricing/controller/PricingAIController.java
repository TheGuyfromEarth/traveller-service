package com.travolish.traveller.pricing.controller;

import com.travolish.traveller.pricing.dto.PricingSuggestionDTO;
import com.travolish.traveller.pricing.dto.PricingSuggestionRequest;
import com.travolish.traveller.pricing.service.PricingAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/pricing/suggestions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class PricingAIController {

    private final PricingAIService pricingAIService;

    /**
     * Generate pricing suggestion using AI
     * POST /api/pricing/suggestions/generate
     */
    @PostMapping("/generate")
    public ResponseEntity<List<PricingSuggestionDTO>> generateSuggestion(
            @Valid @RequestBody PricingSuggestionRequest request) {
        try {
            if (request.getRoomId() != null) {
                log.info("Generating pricing suggestion for room: {}", request.getRoomId());
                PricingSuggestionDTO suggestion = pricingAIService.generateSuggestion(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(List.of(suggestion));
            }
            log.info("Generating pricing suggestions for hotel: {}", request.getHotelId());
            List<PricingSuggestionDTO> suggestions = pricingAIService.generateSuggestionsForHotel(request.getHotelId());
            return ResponseEntity.status(HttpStatus.CREATED).body(suggestions);
        } catch (Exception e) {
            log.error("Error generating pricing suggestion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all suggestions for hotel
     * GET /api/pricing/suggestions/hotel/{hotelId}
     */
    @GetMapping("/hotel/{hotelId}")
    public ResponseEntity<List<PricingSuggestionDTO>> getSuggestionsForHotel(
            @PathVariable Long hotelId) {
        List<PricingSuggestionDTO> suggestions = pricingAIService.getSuggestionsForHotel(hotelId);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get all suggestions for room
     * GET /api/pricing/suggestions/room/{roomId}
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<List<PricingSuggestionDTO>> getSuggestionsForRoom(
            @PathVariable Long roomId) {
        List<PricingSuggestionDTO> suggestions = pricingAIService.getSuggestionsForRoom(roomId);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Get pending suggestions (paginated)
     * GET /api/pricing/suggestions/pending
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<PricingSuggestionDTO>> getPendingSuggestions(
            @RequestParam Long hotelId,
            Pageable pageable) {
        Page<PricingSuggestionDTO> suggestions = pricingAIService.getPendingSuggestionsForHotel(hotelId, pageable);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * Accept pricing suggestion
     * POST /api/pricing/suggestions/{suggestionId}/accept
     */
    @PostMapping("/{suggestionId}/accept")
    public ResponseEntity<PricingSuggestionDTO> acceptSuggestion(
            @PathVariable Long suggestionId) {
        try {
            log.info("Accepting pricing suggestion: {}", suggestionId);
            PricingSuggestionDTO suggestion = pricingAIService.acceptSuggestion(suggestionId);
            return ResponseEntity.ok(suggestion);
        } catch (Exception e) {
            log.error("Error accepting suggestion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reject pricing suggestion
     * POST /api/pricing/suggestions/{suggestionId}/reject
     */
    @PostMapping("/{suggestionId}/reject")
    public ResponseEntity<PricingSuggestionDTO> rejectSuggestion(
            @PathVariable Long suggestionId,
            @RequestParam(required = false) String reason) {
        try {
            log.info("Rejecting pricing suggestion: {}", suggestionId);
            PricingSuggestionDTO suggestion = pricingAIService.rejectSuggestion(suggestionId, reason);
            return ResponseEntity.ok(suggestion);
        } catch (Exception e) {
            log.error("Error rejecting suggestion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Analyze demand trends
     * GET /api/pricing/suggestions/analyze/demand
     */
    @GetMapping("/analyze/demand")
    public ResponseEntity<List<PricingSuggestionDTO>> analyzeDemandTrends(
            @RequestParam Long hotelId) {
        List<PricingSuggestionDTO> analysis = pricingAIService.analyzeDemandTrends(hotelId);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Analyze competitor pricing
     * GET /api/pricing/suggestions/analyze/competitors
     */
    @GetMapping("/analyze/competitors")
    public ResponseEntity<List<PricingSuggestionDTO>> analyzeCompetitorPricing(
            @RequestParam Long hotelId) {
        List<PricingSuggestionDTO> analysis = pricingAIService.analyzeCompetitorPricing(hotelId);
        return ResponseEntity.ok(analysis);
    }

    /**
     * Generate seasonal pricing suggestions
     * GET /api/pricing/suggestions/seasonal
     */
    @GetMapping("/seasonal")
    public ResponseEntity<List<PricingSuggestionDTO>> generateSeasonalSuggestions(
            @RequestParam Long hotelId) {
        List<PricingSuggestionDTO> suggestions = pricingAIService.generateSeasonalPricingSuggestions(hotelId);
        return ResponseEntity.ok(suggestions);
    }
}
